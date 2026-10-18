package com.toolsboox.plugin.calendar.ot

import com.toolsboox.da.Stroke
import com.toolsboox.da.StrokePoint
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CalendarTaskCarryOverTest {

    // Tasks section geometry of the Default day page (see CalendarTaskCarryOver).
    private val rowHeight = 50f
    private val tasksTop = (1872f - 35 * rowHeight) / 2f + rowHeight
    private val textLeft = 760f
    private val descenderX = textLeft + 80f
    private val checkboxX = 685f

    private fun rowTop(row: Int) = tasksTop + (row - 1) * rowHeight

    /** A stroke from [top] to [bottom] (page coordinates), starting at the top, around [x]. */
    private fun stroke(x: Float, top: Float, bottom: Float) = Stroke(
        UUID.randomUUID(), 0L,
        listOf(StrokePoint(x, top, 1f, 0L), StrokePoint(x + 10f, (top + bottom) / 2, 1f, 1L), StrokePoint(x, bottom, 1f, 2L))
    )

    /** A short word in the middle of [row]: three letters of x-height. */
    private fun word(row: Int) = (0 until 3).map { i -> stroke(textLeft + i * 25f, rowTop(row) + 15f, rowTop(row) + 40f) }

    /** An f starting in [row] and reaching far into the row below. */
    private fun descender(row: Int) = stroke(descenderX, rowTop(row) + 5f, rowTop(row + 1) + 35f)

    private fun day(day: Int, strokes: List<Stroke>) = CalendarDay(
        2026, 10, day, startHour = 5, calendarStrokes = mutableMapOf(CalendarDay.DEFAULT_STYLE to strokes)
    )

    private fun CalendarDay.strokes() = calendarStrokes[CalendarDay.DEFAULT_STYLE]!!

    private fun List<Stroke>.descenderStroke() = single { it.strokePoints.first().x == descenderX }

    @Test
    fun descenderMovesWithItsWordInsteadOfBecomingATask() {
        val yesterday = day(17, word(2) + descender(2))
        val today = day(18, emptyList())

        assertTrue(CalendarTaskCarryOver.carryOver(yesterday, today))

        // One task, moved from row 2 to row 1 together with its f.
        assertEquals(4, today.strokes().size)
        assertTrue(yesterday.strokes().isEmpty())
        val f = today.strokes().descenderStroke()
        assertEquals(rowTop(1) + 5f, f.strokePoints.first().y, 0.01f)
        assertEquals(rowTop(2) + 35f, f.strokePoints.last().y, 0.01f)
    }

    @Test
    fun descenderAboveAnotherTaskStaysWithTheRowItStartsIn() {
        val yesterday = day(17, word(2) + descender(2) + word(3))
        val today = day(18, emptyList())

        assertTrue(CalendarTaskCarryOver.carryOver(yesterday, today))

        // Two tasks, moved to rows 1 and 2; the f is still part of the upper one.
        assertEquals(7, today.strokes().size)
        assertEquals(rowTop(1) + 5f, today.strokes().descenderStroke().strokePoints.first().y, 0.01f)
    }

    @Test
    fun descenderWrittenAsSecondStrokeStaysWithItsWord() {
        // Geometry of a real "Abfluss": the f's stem reaches 17 units into row 3, its lower part is a second
        // stroke lying almost entirely in the top of row 3.
        val stem = stroke(descenderX, rowTop(2) + 4f, rowTop(3) + 17f)
        val lowerPart = Stroke(
            UUID.randomUUID(), 0L,
            listOf(
                StrokePoint(descenderX - 15f, rowTop(3) + 13f, 1f, 0L),
                StrokePoint(descenderX, rowTop(3) - 2f, 1f, 1L),
                StrokePoint(descenderX + 16f, rowTop(3) + 5f, 1f, 2L)
            )
        )
        val yesterday = day(17, word(2) + stem + lowerPart)
        val today = day(18, emptyList())

        assertTrue(CalendarTaskCarryOver.carryOver(yesterday, today))

        // One task: everything moved up by exactly one row, nothing stayed behind as a second "task".
        assertEquals(5, today.strokes().size)
        assertTrue(yesterday.strokes().isEmpty())
        val movedLowerPart = today.strokes().single { it.strokePoints.first().x == descenderX - 15f }
        assertEquals(rowTop(2) + 13f, movedLowerPart.strokePoints.first().y, 0.01f)
    }

    @Test
    fun dotAboveTheLineBelongsToTheWordBelow() {
        // An i-dot written into the otherwise empty row 3, above a word in row 4.
        val dot = Stroke(
            UUID.randomUUID(), 0L,
            listOf(StrokePoint(textLeft + 30f, rowTop(4) - 5f, 1f, 0L), StrokePoint(textLeft + 32f, rowTop(4) - 3f, 1f, 1L))
        )
        val existing = word(1)
        val yesterday = day(17, word(4) + dot)
        val today = day(18, existing)

        assertTrue(CalendarTaskCarryOver.carryOver(yesterday, today))

        // Only one task (row 4 with its dot) moved, into the first free row 2; the dot did not make row 3 a task.
        val moved = today.strokes() - existing.toSet()
        assertEquals(4, moved.size)
        assertTrue(moved.all { s -> s.strokePoints.all { it.y > rowTop(2) - 10f && it.y < rowTop(3) } })
    }

    @Test
    fun checkedTaskStays() {
        val check = stroke(checkboxX, rowTop(2) + 15f, rowTop(2) + 35f)
        val yesterday = day(17, word(2) + check)
        val today = day(18, emptyList())

        assertFalse(CalendarTaskCarryOver.carryOver(yesterday, today))
        assertEquals(4, yesterday.strokes().size)
    }
}
