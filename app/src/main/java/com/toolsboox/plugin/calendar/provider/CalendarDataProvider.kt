package com.toolsboox.plugin.calendar.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.toolsboox.da.Stroke
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.fi.CalendarDayService
import com.toolsboox.plugin.calendar.fi.CalendarDayStore
import com.toolsboox.plugin.calendar.fi.CalendarPatternService
import com.toolsboox.plugin.calendar.nw.CalendarWebDavSyncWorker
import com.toolsboox.plugin.calendar.ui.CalendarWebDavSyncPresenter
import com.toolsboox.plugin.calendar.widget.CalendarWidgetProvider
import com.toolsboox.ui.plugin.FragmentPresenter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

/**
 * Gives apps signed with the same key (the ink launcher) access to the calendar days.
 *
 * - `openFile(content://<applicationId>.data/day/2026-10-18, "r")`: the day as v2 JSON (carries over open tasks if today)
 * - `call("putStrokes", "2026-10-18", {style, strokes})`: replaces the strokes of one page style, strokes as JSON array
 *   in the unified 1404x1872 canvas coordinates
 * - `call("sync")`: starts a WebDAV sync now
 * - `call("syncStatus")`: outcome of the last sync ({configured, started, finished, ok, error})
 *
 * Every saved day is announced via `notifyChange` on its day URI, so observers of `/day` (with descendants) can reload.
 * Sync start and end are announced on `/sync`.
 */
class CalendarDataProvider : ContentProvider() {

    companion object {
        const val PERMISSION = "com.overwatcher.programmieramt.permission.CALENDAR_DATA"

        const val METHOD_PUT_STROKES = "putStrokes"
        const val METHOD_SYNC = "sync"
        const val METHOD_SYNC_STATUS = "syncStatus"

        const val EXTRA_STYLE = "style"
        const val EXTRA_STROKES = "strokes"
        const val EXTRA_UPDATED = "updated"

        private const val PATH_DAY = "day"
        private const val PATH_SYNC = "sync"

        /**
         * Delay of the WebDAV sync after a write, so a burst of strokes is uploaded once.
         */
        private const val SYNC_DELAY_SECONDS = 20L

        fun dayUri(context: Context, date: LocalDate): Uri =
            Uri.parse("content://${context.packageName}.data/$PATH_DAY/$date")

        fun notifyDayChanged(context: Context, date: LocalDate) {
            context.contentResolver.notifyChange(dayUri(context, date), null)
        }

        fun notifySyncChanged(context: Context) {
            context.contentResolver.notifyChange(Uri.parse("content://${context.packageName}.data/$PATH_SYNC"), null)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        fun calendarDayService(): CalendarDayService
        fun calendarPatternService(): CalendarPatternService
        fun calendarDayStore(): CalendarDayStore
        fun sharedPreferences(): SharedPreferences
        fun moshi(): Moshi
        fun syncPresenter(): CalendarWebDavSyncPresenter
    }

    /**
     * Resolved lazily: Hilt's application component is not ready yet in onCreate.
     */
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(requireContext().applicationContext, ProviderEntryPoint::class.java)
    }

    private val strokesAdapter: JsonAdapter<List<Stroke>> by lazy {
        entryPoint.moshi().adapter(Types.newParameterizedType(List::class.java, Stroke::class.java))
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = if (parseDay(uri) != null) "application/json" else null

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Read only: $uri")
        val date = parseDay(uri) ?: throw FileNotFoundException("Unknown uri: $uri")

        val context = requireContext()
        val rootPath = FragmentPresenter.documentsRoot(context)
        val startHour = defaultStartHour()
        val locale = locale()

        val json = runBlocking(Dispatchers.IO) {
            val (calendarDay, calendarPattern) = CalendarPatternService.mutex.withLock {
                entryPoint.calendarDayService().load(rootPath, date, startHour, locale) to
                        entryPoint.calendarPatternService().load(rootPath, date, locale)
            }
            entryPoint.calendarDayStore()
                .carryOverTasks(context, rootPath, date, calendarDay, calendarPattern, startHour, locale)
            entryPoint.calendarDayService().json(calendarDay)
        }

        // Unlinked right after opening: the descriptor stays readable, nothing is left in the cache.
        val file = File.createTempFile("day-", ".json", context.cacheDir)
        file.writeText(json)
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        file.delete()
        return descriptor
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = requireContext()
        // Unlike query/openFile, call() is not covered by the provider's android:permission.
        if (context.checkCallingOrSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("$PERMISSION required")
        }

        return when (method) {
            METHOD_PUT_STROKES -> putStrokes(context, arg, extras)
            METHOD_SYNC -> {
                CalendarWebDavSyncWorker.enqueue(context, 0L)
                Bundle()
            }

            METHOD_SYNC_STATUS -> {
                val status = entryPoint.syncPresenter().status()
                Bundle().apply {
                    putBoolean("configured", status.configured)
                    putLong("started", status.started)
                    putLong("finished", status.finished)
                    putBoolean("ok", status.ok)
                    putString("error", status.error)
                }
            }

            else -> throw IllegalArgumentException("Unknown method: $method")
        }
    }

    private fun putStrokes(context: Context, arg: String?, extras: Bundle?): Bundle {
        val date = arg?.let { parseDate(it) } ?: throw IllegalArgumentException("Invalid date: $arg")
        val style = extras?.getString(EXTRA_STYLE) ?: CalendarDay.DEFAULT_STYLE
        val strokesJson = extras?.getString(EXTRA_STROKES) ?: throw IllegalArgumentException("Missing $EXTRA_STROKES")
        val strokes = strokesAdapter.fromJson(strokesJson) ?: throw IllegalArgumentException("Invalid $EXTRA_STROKES")

        val rootPath = FragmentPresenter.documentsRoot(context)
        val calendarDay = runBlocking(Dispatchers.IO) {
            entryPoint.calendarDayStore()
                .saveStrokes(context, rootPath, date, style, strokes, defaultStartHour(), locale())
        }
        Timber.i("Saved ${strokes.size} strokes of $date/$style from provider")

        CalendarWidgetProvider.refreshAll(context)
        CalendarWebDavSyncWorker.enqueue(context, SYNC_DELAY_SECONDS)

        return Bundle().apply { putLong(EXTRA_UPDATED, calendarDay.updated?.time ?: 0L) }
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun defaultStartHour(): Int = entryPoint.sharedPreferences().getInt("calendarStartHour", 5)

    /**
     * Same locale resolution as the day view.
     */
    private fun locale(): Locale {
        val tag = entryPoint.sharedPreferences().getString("calendarLocale", null) ?: return Locale.getDefault()
        val locale = Locale.forLanguageTag(tag)
        return if (locale.toLanguageTag() == tag) locale else Locale.getDefault()
    }

    private fun parseDay(uri: Uri): LocalDate? {
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != PATH_DAY) return null
        return parseDate(segments[1])
    }

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
        null
    }
}
