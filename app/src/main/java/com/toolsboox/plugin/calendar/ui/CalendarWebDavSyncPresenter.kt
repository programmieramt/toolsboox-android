package com.toolsboox.plugin.calendar.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.lifecycleScope
import com.toolsboox.fi.WebDavService
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncItem
import com.toolsboox.plugin.calendar.da.v1.CalendarSyncViewItem
import com.toolsboox.plugin.calendar.fi.*
import com.toolsboox.plugin.calendar.provider.CalendarDataProvider
import com.toolsboox.ui.plugin.FragmentPresenter
import com.toolsboox.ui.plugin.ScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

class CalendarWebDavSyncPresenter @Inject constructor() : FragmentPresenter() {

    companion object {
        /**
         * Serializes syncs started by the day view and by the background worker.
         */
        private val syncMutex = Mutex()

        private const val PREF_SYNC_STARTED = "webDavSyncStarted"
        private const val PREF_SYNC_FINISHED = "webDavSyncFinished"
        private const val PREF_SYNC_OK = "webDavSyncOk"
        private const val PREF_SYNC_ERROR = "webDavSyncError"
    }

    /**
     * Outcome of the last sync, for the Paper Launch indicator (see CalendarDataProvider).
     *
     * @property configured WebDAV is set up and auto sync is on
     * @property started epoch millis of the last start, 0 if never
     * @property finished epoch millis of the last end, 0 if never; a sync runs while [started] > [finished]
     * @property error message of the last failure, null if the last sync succeeded
     */
    data class SyncStatus(
        val configured: Boolean, val started: Long, val finished: Long, val ok: Boolean, val error: String?
    )

    fun status(): SyncStatus = SyncStatus(
        configured = autoSyncEnabled() && webDavConfigured(),
        started = sharedPreferences.getLong(PREF_SYNC_STARTED, 0L),
        finished = sharedPreferences.getLong(PREF_SYNC_FINISHED, 0L),
        ok = sharedPreferences.getBoolean(PREF_SYNC_OK, false),
        error = sharedPreferences.getString(PREF_SYNC_ERROR, null)
    )

    private fun autoSyncEnabled() = sharedPreferences.getString("webDavAutoSyncOptIn", "true").toBoolean()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var calendarDayService: CalendarDayService

    @Inject
    lateinit var calendarMonthService: CalendarMonthService

    @Inject
    lateinit var calendarPatternService: CalendarPatternService

    @Inject
    lateinit var calendarQuarterService: CalendarQuarterService

    @Inject
    lateinit var calendarYearService: CalendarYearService

    private fun webDavUrl() = sharedPreferences.getString("webDavUrl", "") ?: ""
    private fun webDavUser() = sharedPreferences.getString("webDavUser", "") ?: ""
    private fun webDavPassword() = sharedPreferences.getString("webDavPassword", "") ?: ""
    private fun webDavConfigured() = webDavUrl().isNotEmpty() && webDavUser().isNotEmpty()
    private fun webDavTrustAllCerts() = sharedPreferences.getBoolean("webDavSyncTrustAllCerts", false)

    fun backgroundSync(fragment: ScreenFragment, userId: UUID) {
        if (!autoSyncEnabled()) return
        if (!webDavConfigured()) return

        val lastSync = Instant.ofEpochMilli(sharedPreferences.getLong("lastCalendarWebDavBackgroundSync", 0L))
        if (Instant.now().minusSeconds(30).isBefore(lastSync)) return
        sharedPreferences.edit().putLong("lastCalendarWebDavBackgroundSync", Instant.now().toEpochMilli()).apply()

        val appContext = fragment.requireContext().applicationContext
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            sync(appContext, userId)
        }
    }

    /**
     * Bidirectional sync of calendar data and flat notes, without fragment (also used by [CalendarWebDavSyncWorker]).
     */
    suspend fun sync(context: Context, userId: UUID) {
        if (!autoSyncEnabled()) return
        if (!webDavConfigured()) return

        syncMutex.withLock {
            sharedPreferences.edit().putLong(PREF_SYNC_STARTED, System.currentTimeMillis()).commit()
            CalendarDataProvider.notifySyncChanged(context)

            val error: String? = try {
                val rootPath = documentsRoot(context)
                val fileList = fileList(rootPath, userId)
                val cloudList = cloudList()
                val syncList = calculateSyncList(fileList, cloudList)

                Timber.i("WebDAV background sync items: $syncList")
                syncList.forEach { item ->
                    val fileLastModified = item.file?.updated?.time ?: 0L
                    val cloudLastModified = item.cloud?.updated?.time ?: 0L
                    if (fileLastModified < cloudLastModified) {
                        Timber.i("File update from WebDAV: ${item.cloud}")
                        fileUpdate(context, rootPath, cloudLoad(item.cloud!!))
                    } else {
                        Timber.i("WebDAV update: ${item.file}")
                        cloudUpdate(fileLoad(rootPath, item.file!!))
                    }
                }

                syncFlatNotes(rootPath, "sofort")
                syncFlatNotes(rootPath, "oneonone")
                null
            } catch (e: IOException) {
                Timber.e(e, "WebDAV background sync failed")
                e.message ?: e.javaClass.simpleName
            } catch (e: Exception) {
                Timber.e(e, "WebDAV background sync error")
                e.message ?: e.javaClass.simpleName
            }

            sharedPreferences.edit()
                .putLong(PREF_SYNC_FINISHED, System.currentTimeMillis())
                .putBoolean(PREF_SYNC_OK, error == null)
                .putString(PREF_SYNC_ERROR, error)
                .commit()
            CalendarDataProvider.notifySyncChanged(context)
        }
    }

    private fun syncFlatNotes(rootPath: File, subDir: String) {
        val localDir = File(rootPath, subDir)
        val url = webDavUrl()
        val user = webDavUser()
        val pass = webDavPassword()
        val trustAll = webDavTrustAllCerts()

        val localFiles = (localDir.listFiles { f -> f.name.endsWith(".json") } ?: emptyArray())
            .associate { it.name to it.lastModified() }

        val cloudFiles = try {
            WebDavService.list(url, subDir, user, pass, trustAll)
                .filter { !it.isDirectory && it.name.endsWith(".json") }
                .associate { it.name to it.lastModified }
        } catch (e: Exception) {
            Timber.w(e, "WebDAV list failed for $subDir")
            emptyMap()
        }

        WebDavService.mkdirs(url, subDir, user, pass, trustAll)

        localFiles.forEach { (name, localTime) ->
            val cloudTime = cloudFiles[name] ?: 0L
            if (localTime > cloudTime) {
                val bytes = File(localDir, name).readBytes()
                WebDavService.put(url, "$subDir/$name", bytes, user, pass, trustAll)
                Timber.i("WebDAV upload $subDir/$name")
            }
        }

        cloudFiles.forEach { (name, cloudTime) ->
            val localTime = localFiles[name] ?: 0L
            if (cloudTime > localTime) {
                val bytes = WebDavService.get(url, "$subDir/$name", user, pass, trustAll)
                localDir.mkdirs()
                File(localDir, name).writeBytes(bytes)
                Timber.i("WebDAV download $subDir/$name")
            }
        }
    }

    private fun cloudList(): MutableList<CalendarSyncItem> {
        val items = mutableListOf<CalendarSyncItem>()
        val url = webDavUrl()
        val user = webDavUser()
        val pass = webDavPassword()

        val trustAllCerts = webDavTrustAllCerts()

        fun walkFolder(path: String) {
            val entries = WebDavService.list(url, "calendar/$path", user, pass, trustAllCerts)
            entries.forEach { entry ->
                if (entry.isDirectory) {
                    val subPath = "$path${entry.name}/"
                    walkFolder(subPath)
                } else if (entry.name.endsWith(".json")) {
                    // fileName format: {baseName}-{version}.json
                    val nameWithoutExt = entry.name.removeSuffix(".json")
                    val lastDash = nameWithoutExt.lastIndexOf('-')
                    if (lastDash < 0) return@forEach
                    val baseName = nameWithoutExt.substring(0, lastDash)
                    val version = nameWithoutExt.substring(lastDash + 1)
                    items.add(CalendarSyncItem(UUID.randomUUID(), path, baseName, version, Date(entry.lastModified), Date(entry.lastModified), null))
                }
            }
        }

        // Network errors (e.g. server not reachable via Tailscale) fail the sync; HTTP errors yield an empty list.
        walkFolder("")
        return items
    }

    private fun cloudLoad(item: CalendarSyncItem): CalendarSyncItem {
        val fileName = "${item.baseName}-${item.version}.json"
        val bytes = WebDavService.get(
            webDavUrl(), "calendar/${item.path}$fileName", webDavUser(), webDavPassword(), webDavTrustAllCerts()
        )
        item.json = String(bytes, Charsets.UTF_8)
        return item
    }

    private fun cloudUpdate(item: CalendarSyncItem) {
        val fileName = "${item.baseName}-${item.version}.json"
        val path = "calendar/${item.path}"
        val trustAllCerts = webDavTrustAllCerts()
        WebDavService.mkdirs(webDavUrl(), path, webDavUser(), webDavPassword(), trustAllCerts)
        WebDavService.put(
            webDavUrl(), "$path$fileName", (item.json ?: "").toByteArray(Charsets.UTF_8),
            webDavUser(), webDavPassword(), trustAllCerts
        )
    }

    private fun fileList(rootPath: File, userId: UUID): MutableList<CalendarSyncItem> {
        val items: MutableList<CalendarSyncItem> = mutableListOf()
        val path = File(rootPath, "calendar/")
        if (!path.exists()) return items

        Files.walk(Paths.get(path.toURI())).use { stream ->
            stream.map(Path::toFile).filter(File::isFile).filter { it.name.endsWith(".json") }.forEach { item ->
                if (item.name.startsWith("pattern-")) return@forEach
                calendarYearService.load(item)?.let { items.add(calendarYearService.getItem(userId, it)) }
                calendarQuarterService.load(item)?.let { items.add(calendarQuarterService.getItem(userId, it)) }
                calendarMonthService.load(item)?.let { items.add(calendarMonthService.getItem(userId, it)) }
                calendarDayService.load(item)?.let { items.add(calendarDayService.getItem(userId, it)) }
            }
        }
        return items
    }

    private fun fileLoad(rootPath: File, item: CalendarSyncItem): CalendarSyncItem {
        calendarYearService.load(rootPath, item.path, item.baseName)?.let { item.json = calendarYearService.json(it) }
        calendarQuarterService.load(rootPath, item.path, item.baseName)?.let { item.json = calendarQuarterService.json(it) }
        calendarMonthService.load(rootPath, item.path, item.baseName)?.let { item.json = calendarMonthService.json(it) }
        calendarDayService.load(rootPath, item.path, item.baseName)?.let { item.json = calendarDayService.json(it) }
        return item
    }

    private fun fileUpdate(context: Context, rootPath: File, item: CalendarSyncItem): CalendarSyncItem {
        calendarYearService.fromSyncItem(item)?.let {
            calendarYearService.save(rootPath, item.path, item.baseName, it)
            val currentDate = LocalDate.ofYearDay(it.year, 1)
            val pattern = calendarPatternService.load(rootPath, currentDate, it.locale)
            pattern.updateYear(it)
            calendarPatternService.save(rootPath, currentDate, pattern)
        }
        calendarQuarterService.fromSyncItem(item)?.let {
            calendarQuarterService.save(rootPath, item.path, item.baseName, it)
            val currentDate = LocalDate.ofYearDay(it.year, 1)
            val pattern = calendarPatternService.load(rootPath, currentDate, it.locale)
            pattern.updateQuarter(it)
            calendarPatternService.save(rootPath, currentDate, pattern)
        }
        calendarMonthService.fromSyncItem(item)?.let {
            calendarMonthService.save(rootPath, item.path, item.baseName, it)
            val currentDate = LocalDate.ofYearDay(it.year, 1)
            val pattern = calendarPatternService.load(rootPath, currentDate, it.locale)
            pattern.updateMonth(it)
            calendarPatternService.save(rootPath, currentDate, pattern)
        }
        calendarDayService.fromSyncItem(item)?.let {
            calendarDayService.save(rootPath, item.path, item.baseName, it)
            val currentDate = LocalDate.ofYearDay(it.year, 1)
            val pattern = calendarPatternService.load(rootPath, currentDate, it.locale)
            pattern.updateDay(it)
            calendarPatternService.save(rootPath, currentDate, pattern)
            CalendarDataProvider.notifyDayChanged(context, LocalDate.of(it.year, it.month, it.day))
        }
        return item
    }

    fun calculateSyncList(fileList: List<CalendarSyncItem>, cloudList: List<CalendarSyncItem>): List<CalendarSyncViewItem> {
        val syncList = mutableListOf<CalendarSyncViewItem>()

        fileList.forEach { fci ->
            if (fci.updated == null) {
                syncList.add(CalendarSyncViewItem(fci, fci, null))
                return@forEach
            }
            val cloudItem = cloudList
                .filter { it.path == fci.path }
                .filter { it.baseName == fci.baseName }
                .firstOrNull { it.version == fci.version }

            if (cloudItem == null) {
                syncList.add(CalendarSyncViewItem(fci, fci, null))
                return@forEach
            }
            if ((cloudItem.updated != null) && (cloudItem.updated!!.time < fci.updated.time)) {
                syncList.add(CalendarSyncViewItem(fci, fci, cloudItem))
            }
        }

        cloudList.forEach { cci ->
            if (cci.updated == null) return@forEach
            val fileItem = fileList
                .filter { it.path == cci.path }
                .filter { it.baseName == cci.baseName }
                .firstOrNull { it.version == cci.version }

            if (fileItem == null) {
                syncList.add(CalendarSyncViewItem(cci, null, cci))
            } else if (fileItem.updated != null && fileItem.updated.time < cci.updated.time) {
                syncList.add(CalendarSyncViewItem(fileItem, fileItem, cci))
            }
        }

        return syncList
    }
}
