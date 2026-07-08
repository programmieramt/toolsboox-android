package com.toolsboox.plugin.sofort.ui

import android.os.Environment
import com.toolsboox.da.Stroke
import com.toolsboox.plugin.sofort.da.SofortNote
import com.toolsboox.plugin.sofort.fi.SofortService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class SofortPresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var sofortService: SofortService

    fun load(fragment: SofortFragment, id: UUID) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.showLoading() }
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val note = sofortService.load(rootPath, id)
                withContext(Dispatchers.Main) { fragment.renderNote(note) }
            } finally {
                withContext(Dispatchers.Main) { fragment.hideLoading() }
            }
        }
    }

    fun save(fragment: SofortFragment, note: SofortNote, strokes: List<Stroke>) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val updated = note.copy(strokes = strokes, updatedAt = System.currentTimeMillis())
                sofortService.save(rootPath, updated)
                Timber.i("SofortNote saved: ${note.id}, strokes: ${strokes.size}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save SofortNote ${note.id}")
                withContext(Dispatchers.Main) {
                    fragment.runOnActivity { fragment.somethingHappened(e, fragment.provideSurfaceView()) }
                }
            }
        }
    }
}
