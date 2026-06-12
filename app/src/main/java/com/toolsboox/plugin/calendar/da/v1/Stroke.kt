package com.toolsboox.plugin.calendar.da.v1

import com.squareup.moshi.JsonClass
import com.toolsboox.da.StrokePoint
import java.util.*

/**
 * Stroke data class (legacy v1 format).
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class Stroke(
    var pageId: UUID,
    var strokeId: UUID,
    var strokePoints: List<StrokePoint>
) {
    companion object {
        /**
         * Convert strokes from common format to v1 format.
         *
         * @param commonStrokes the list of strokes in common format
         * @param pageId the page id to add
         * @return list of strokes in v1 format
         */
        fun convertFrom(commonStrokes: List<com.toolsboox.da.Stroke>, pageId: UUID): List<Stroke> {
            val strokes = mutableListOf<Stroke>()
            commonStrokes.forEach { s -> strokes.add(Stroke(pageId, s.strokeId, s.strokePoints)) }

            return strokes.toList()
        }

        /**
         * Convert strokes from v1 format to common format.
         *
         * @param teamStrokes the list of strokes in v1 format
         * @return list of strokes in common format
         */
        fun convertTo(teamStrokes: List<Stroke>): List<com.toolsboox.da.Stroke> {
            val strokes = mutableListOf<com.toolsboox.da.Stroke>()
            teamStrokes.forEach { s -> strokes.add(com.toolsboox.da.Stroke(s.strokeId, 0L, s.strokePoints)) }

            return strokes.toList()
        }
    }
}
