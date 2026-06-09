package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.toolsboox.R
import com.toolsboox.ot.Creator
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v1.CalendarPattern
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

/**
 * Create navigator of daily template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarDayNavigator {

    companion object {
        private const val slotsLeft = 140f
        private const val slotsRight = 1264f
        private const val slotCount = 5

        /**
         * Process touch event on the calendar navigator and navigate to the view of calendar.
         *
         * @param view the surface view
         * @param motionEvent the motion event
         * @param fragment the parent fragment
         * @param calendarDay the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, fragment: CalendarDayFragment, calendarDay: CalendarDay
        ): Boolean {
            val localDate = LocalDate.of(calendarDay.year, calendarDay.month, calendarDay.day)

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    val px = motionEvent.x * 1404.0f / view.width
                    val py = motionEvent.y * 140.4f / view.height

                    val slotWidth = (slotsRight - slotsLeft) / slotCount

                    if (py < 0f || py > 140.4f) return true

                    if (px < slotsLeft) {
                        CalendarNavigator.toDayPage(fragment, localDate.minusDays(1L))
                        return true
                    }
                    if (px < slotsLeft + 2 * slotWidth) {
                        CalendarNavigator.toDayPage(fragment, localDate)
                        return true
                    }
                    if (px < slotsLeft + 3 * slotWidth) {
                        CalendarNavigator.toMonthPage(fragment, localDate)
                        return true
                    }
                    if (px < slotsLeft + 4 * slotWidth) {
                        CalendarNavigator.toQuarterPage(fragment, localDate)
                        return true
                    }
                    if (px < slotsRight) {
                        CalendarNavigator.toYearPage(fragment, localDate)
                        return true
                    }
                    CalendarNavigator.toDayPage(fragment, localDate.plusDays(1L))
                    return true
                }
            }

            return true
        }

        /**
         * Draw the navigator of daily template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarDay data class
         * @param calendarPattern the calendar pattern
         */
        fun draw(context: Context, canvas: Canvas, calendarDay: CalendarDay, calendarPattern: CalendarPattern) {
            val currentDate = LocalDate.of(calendarDay.year, calendarDay.month, calendarDay.day)

            val year = currentDate.year
            val dayOfYear = currentDate.dayOfYear
            val monthOfYear = currentDate.monthValue
            val monthName = currentDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val quarterOfYear = (currentDate.monthValue - 1) / 3 + 1
            val day = currentDate.dayOfMonth
            val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            NavigatorRenderer.render(context, canvas, listOf(
                NavigatorRenderer.Slot("$day", NavigatorRenderer.Emphasis.FOCAL,
                    calendarPattern.getDayPages(dayOfYear) > 0, calendarPattern.getDayNotes(dayOfYear)),
                NavigatorRenderer.Slot(dayOfWeek, NavigatorRenderer.Emphasis.NORMAL, false, 0),
                NavigatorRenderer.Slot(monthName, NavigatorRenderer.Emphasis.NORMAL,
                    calendarPattern.getMonthPages(monthOfYear) > 0, calendarPattern.getMonthNotes(monthOfYear)),
                NavigatorRenderer.Slot(context.getString(R.string.quarter_abbreviation, quarterOfYear),
                    NavigatorRenderer.Emphasis.MUTED,
                    calendarPattern.getQuarterPages(quarterOfYear) > 0, calendarPattern.getQuarterNotes(quarterOfYear)),
                NavigatorRenderer.Slot("$year", NavigatorRenderer.Emphasis.MUTED,
                    calendarPattern.getYearPages() > 0, calendarPattern.getYearNotes()),
            ))
        }
    }
}
