package com.toolsboox.plugin.calendar.ot

import com.toolsboox.da.Stroke
import com.toolsboox.plugin.calendar.da.v2.CalendarDay

/**
 * Carries over open (unchecked) rows of the Tasks section of the Default day page
 * to the next day's page.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
object CalendarTaskCarryOver {

    // Layout constants of the Tasks section on the Default day page, see CalendarDayPage
    private const val CEW = 600.0f
    private const val CEH = 50.0f
    private const val LO = 20.0f
    private const val TO = (1872.0f - 35 * CEH) / 2.0f

    private const val TASKS_LEFT = LO + CEW + 50.0f
    private const val TASKS_RIGHT = LO + 2 * CEW + 50.0f
    private const val TASKS_DIVIDER = LO + CEW + 100.0f
    private const val TASKS_TOP = TO + CEH
    private const val ROWS = 16

    /**
     * Moves open (unchecked) task rows of [yesterday] to the first free rows of [today].
     *
     * @param yesterday the previous day's calendar day, mutated in place
     * @param today the current day's calendar day, mutated in place
     * @return true if any strokes were moved, meaning both days need to be persisted
     */
    fun carryOver(yesterday: CalendarDay, today: CalendarDay): Boolean {
        val sourceStrokes = yesterday.calendarStrokes[CalendarDay.DEFAULT_STYLE] ?: return false
        val targetStrokes = today.calendarStrokes[CalendarDay.DEFAULT_STYLE] ?: listOf()

        val sourceRows = groupByRow(sourceStrokes)
        val targetRows = groupByRow(targetStrokes)

        val openRows = (1..ROWS).filter { row ->
            val strokes = sourceRows[row] ?: return@filter false
            strokes.any { !it.isCheckboxStroke() } && strokes.none { it.isCheckboxStroke() }
        }
        if (openRows.isEmpty()) return false

        val freeRows = (1..ROWS).filter { targetRows[it].isNullOrEmpty() }.toMutableList()
        if (freeRows.isEmpty()) return false

        val movedStrokes = mutableListOf<Stroke>()
        val remainingSourceStrokes = sourceStrokes.toMutableList()

        for (sourceRow in openRows) {
            if (freeRows.isEmpty()) break
            val targetRow = freeRows.removeAt(0)
            val deltaY = (targetRow - sourceRow) * CEH

            val rowStrokes = sourceRows[sourceRow] ?: continue
            rowStrokes.forEach { stroke ->
                movedStrokes.add(stroke.copy(strokePoints = stroke.strokePoints.map { it.copy(y = it.y + deltaY) }))
            }
            remainingSourceStrokes.removeAll(rowStrokes)
        }

        if (movedStrokes.isEmpty()) return false

        yesterday.calendarStrokes[CalendarDay.DEFAULT_STYLE] = remainingSourceStrokes
        today.calendarStrokes[CalendarDay.DEFAULT_STYLE] = targetStrokes + movedStrokes
        return true
    }

    /**
     * A stroke counts as lying in one row if at least this share of its height is inside the row.
     */
    private const val ANCHOR_SHARE = 0.75f

    /**
     * Strokes smaller than this in both directions (dots, accents) never decide a row on their own.
     */
    private const val TINY = 8.0f

    /**
     * A spilling stroke only joins anchors at most this far away horizontally.
     */
    private const val MAX_GAP = 2 * CEH

    private class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private fun Stroke.box() = Box(
        strokePoints.minOf { it.x }, strokePoints.minOf { it.y }, strokePoints.maxOf { it.x }, strokePoints.maxOf { it.y }
    )

    /**
     * Groups the strokes of the Tasks section by their row number (1..16).
     *
     * Text strokes lying mostly within one row ("anchors") belong to that row. Strokes reaching into a neighbouring
     * row (descenders such as f, g, y), tiny marks (i-dots) and pieces hanging in the top half of a row while touching
     * a stroke from the row above (a descender written as a second stroke) join the row they touch whose anchors are
     * horizontally closest, on a tie the row they start in. Otherwise such a stroke would count as a task row of its
     * own. Checkbox marks belong to the row of their center.
     *
     * Same rules as Paper Launch's `OpenTasks.kt`; keep both in sync.
     */
    private fun groupByRow(strokes: List<Stroke>): Map<Int, List<Stroke>> {
        val result = mutableMapOf<Int, MutableList<Stroke>>()
        val anchors = mutableMapOf<Int, MutableList<Box>>()
        val text = mutableListOf<Triple<Stroke, Box, Int>>()

        for (stroke in strokes) {
            if (stroke.strokePoints.isEmpty()) continue
            val centerX = stroke.strokePoints.map { it.x }.average().toFloat()
            val centerY = stroke.strokePoints.map { it.y }.average().toFloat()
            if (centerX < TASKS_LEFT || centerX > TASKS_RIGHT) continue
            val row = rowAt(centerY) ?: continue

            if (stroke.isCheckboxStroke()) {
                result.getOrPut(row) { mutableListOf() }.add(stroke)
            } else {
                text.add(Triple(stroke, stroke.box(), row))
            }
        }

        val spilling = mutableListOf<Triple<Stroke, Box, Int>>()
        for (entry in text) {
            val (stroke, box, row) = entry
            val rowTop = TASKS_TOP + (row - 1) * CEH
            val hanging = box.bottom <= rowTop + CEH / 2 &&
                    text.any { (other, otherBox, _) -> other !== stroke && otherBox.top < rowTop && touches(box, otherBox) }
            if (isAnchor(box, row) && !hanging) {
                result.getOrPut(row) { mutableListOf() }.add(stroke)
                anchors.getOrPut(row) { mutableListOf() }.add(box)
            } else {
                spilling.add(entry)
            }
        }

        for ((stroke, box, centerRow) in spilling) {
            val startRow = rowAt(stroke.strokePoints.first().y) ?: centerRow
            val tiny = box.right - box.left < TINY && box.bottom - box.top < TINY
            val touched = if (tiny) {
                (centerRow - 1)..(centerRow + 1)
            } else {
                minOf(rowAt(box.top) ?: 1, centerRow - 1)..(rowAt(box.bottom) ?: ROWS)
            }

            val row = touched
                .mapNotNull { r -> anchors[r]?.minOf { gap(box, it) }?.let { r to it } }
                .filter { it.second <= MAX_GAP }
                .minWithOrNull(compareBy({ it.second }, { if (it.first == startRow) 0 else 1 }))
                ?.first ?: startRow
            result.getOrPut(row) { mutableListOf() }.add(stroke)
        }

        return result
    }

    /**
     * Returns the Tasks section row number (1..16) of a y coordinate, or null if it's outside of it.
     */
    private fun rowAt(y: Float): Int? {
        if (y < TASKS_TOP || y >= TASKS_TOP + ROWS * CEH) return null
        return ((y - TASKS_TOP) / CEH).toInt() + 1
    }

    private fun isAnchor(box: Box, row: Int): Boolean {
        val height = box.bottom - box.top
        if (box.right - box.left < TINY && height < TINY) return false
        val rowTop = TASKS_TOP + (row - 1) * CEH
        val inside = minOf(box.bottom, rowTop + CEH) - maxOf(box.top, rowTop)
        return inside >= ANCHOR_SHARE * height
    }

    /**
     * Horizontal distance between two boxes, 0 if they overlap horizontally.
     */
    private fun gap(a: Box, b: Box): Float = maxOf(0f, b.left - a.right, a.left - b.right)

    /**
     * Whether two boxes overlap or nearly touch.
     */
    private fun touches(a: Box, b: Box, margin: Float = 3f): Boolean =
        a.left - margin <= b.right && b.left - margin <= a.right && a.top - margin <= b.bottom && b.top - margin <= a.bottom

    /**
     * A stroke counts as a checkbox mark if its center is left of the column divider.
     */
    private fun Stroke.isCheckboxStroke(): Boolean {
        val centerX = strokePoints.map { it.x }.average()
        return centerX < TASKS_DIVIDER
    }
}
