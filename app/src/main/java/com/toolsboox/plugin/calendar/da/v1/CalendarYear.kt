package com.toolsboox.plugin.calendar.da.v1

import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Calendar year data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class CalendarYear(
    val year: Int,
    val locale: Locale = Locale.getDefault(),

    override val strokes: List<Stroke> = listOf(),
    override val notesStrokes: List<Stroke> = listOf()
) : Calendar