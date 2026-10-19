package com.toolsboox.plugin.calendar.fi

import android.content.Context
import com.toolsboox.da.Stroke
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.ot.CalendarTaskCarryOver
import com.toolsboox.plugin.calendar.provider.CalendarDataProvider
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

/**
 * Day operations shared by the day view and the data provider (used by Paper Launch).
 */
class CalendarDayStore @Inject constructor() {

    @Inject
    lateinit var calendarDayService: CalendarDayService

    @Inject
    lateinit var calendarPatternService: CalendarPatternService

    /**
     * Moves the open tasks of yesterday to [calendarDay] if [currentDate] is today, and persists both days.
     *
     * @return true if tasks were moved
     */
    suspend fun carryOverTasks(
        context: Context, rootPath: File, currentDate: LocalDate,
        calendarDay: CalendarDay, calendarPattern: CalendarPattern, defaultStartHour: Int?, locale: Locale
    ): Boolean {
        if (!currentDate.isEqual(LocalDate.now())) return false

        val yesterday = currentDate.minusDays(1)
        val yesterdayCalendarDay = calendarDayService.load(rootPath, yesterday, defaultStartHour, locale)
        if (!CalendarTaskCarryOver.carryOver(yesterdayCalendarDay, calendarDay)) return false

        CalendarPatternService.mutex.withLock {
            val yesterdayPattern = calendarPatternService.load(rootPath, yesterday, locale)
            yesterdayPattern.updateDay(yesterdayCalendarDay)
            calendarDayService.save(rootPath, yesterday, yesterdayCalendarDay)
            calendarPatternService.save(rootPath, yesterday, yesterdayPattern)

            calendarPattern.updateDay(calendarDay)
            calendarDayService.save(rootPath, currentDate, calendarDay)
            calendarPatternService.save(rootPath, currentDate, calendarPattern)
        }

        CalendarDataProvider.notifyDayChanged(context, yesterday)
        CalendarDataProvider.notifyDayChanged(context, currentDate)
        return true
    }

    /**
     * Replaces the strokes of one page style of a day, keeping everything else of the day untouched.
     *
     * @return the saved day
     */
    suspend fun saveStrokes(
        context: Context, rootPath: File, currentDate: LocalDate, style: String,
        strokes: List<Stroke>, defaultStartHour: Int?, locale: Locale
    ): CalendarDay {
        val calendarDay = CalendarPatternService.mutex.withLock {
            val calendarDay = calendarDayService.load(rootPath, currentDate, defaultStartHour, locale)
            val calendarPattern = calendarPatternService.load(rootPath, currentDate, locale)

            calendarDay.calendarStrokes[style] = strokes
            val emptyStrokes = calendarDay.calendarStrokes[CalendarDay.DEFAULT_STYLE]?.isEmpty() ?: true
            calendarDay.hasLanes = calendarDay.hasLanes or emptyStrokes

            calendarPattern.updateDay(calendarDay)
            calendarDayService.save(rootPath, currentDate, calendarDay)
            calendarPatternService.save(rootPath, currentDate, calendarPattern)
            calendarDay
        }

        CalendarDataProvider.notifyDayChanged(context, currentDate)
        return calendarDay
    }
}
