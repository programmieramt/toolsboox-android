package com.toolsboox.plugin.sofort.fi

import com.squareup.moshi.Moshi
import com.toolsboox.plugin.sofort.da.SofortNote
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.util.UUID
import javax.inject.Inject

class SofortService @Inject constructor() {

    @Inject
    lateinit var moshi: Moshi

    private fun notesDir(rootPath: File): File =
        File(rootPath, "sofort").also { it.mkdirs() }

    private fun noteFile(rootPath: File, id: UUID): File =
        File(notesDir(rootPath), "$id.json")

    fun list(rootPath: File): List<SofortNote> {
        val dir = notesDir(rootPath)
        return dir.listFiles { f -> f.name.endsWith(".json") }
            ?.mapNotNull { load(it) }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    fun load(rootPath: File, id: UUID): SofortNote? =
        load(noteFile(rootPath, id))

    fun save(rootPath: File, note: SofortNote) {
        val file = noteFile(rootPath, note.id)
        val json = moshi.adapter(SofortNote::class.java).toJson(note)
        PrintWriter(FileWriter(file)).use { it.write(json) }
        Timber.i("Saved SofortNote ${note.id}")
    }

    fun delete(rootPath: File, id: UUID) {
        noteFile(rootPath, id).delete()
    }

    private fun load(file: File): SofortNote? {
        if (!file.exists()) return null
        return try {
            FileReader(file).use { reader ->
                moshi.adapter(SofortNote::class.java).fromJson(reader.readText())
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load SofortNote from ${file.name}")
            null
        }
    }
}
