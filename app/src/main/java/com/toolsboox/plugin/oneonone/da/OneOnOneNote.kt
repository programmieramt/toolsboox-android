package com.toolsboox.plugin.oneonone.da

import com.squareup.moshi.JsonClass
import com.toolsboox.da.Stroke
import java.util.UUID

@JsonClass(generateAdapter = true)
data class OneOnOneNote(
    val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val strokes: List<Stroke> = emptyList()
)
