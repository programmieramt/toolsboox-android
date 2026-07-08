package com.toolsboox.plugin.oneonone.ui

import android.os.Environment
import com.toolsboox.da.Stroke
import com.toolsboox.plugin.oneonone.da.OneOnOneNote
import com.toolsboox.plugin.oneonone.fi.OneOnOneService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class OneOnOnePresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var oneOnOneService: OneOnOneService

    fun load(fragment: OneOnOneFragment, id: UUID) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.showLoading() }
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val note = oneOnOneService.load(rootPath, id)
                withContext(Dispatchers.Main) { fragment.renderNote(note) }
            } finally {
                withContext(Dispatchers.Main) { fragment.hideLoading() }
            }
        }
    }

    fun save(fragment: OneOnOneFragment, note: OneOnOneNote, strokes: List<Stroke>) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val updated = note.copy(strokes = strokes, updatedAt = System.currentTimeMillis())
                oneOnOneService.save(rootPath, updated)
                Timber.i("OneOnOneNote saved: ${note.id}, strokes: ${strokes.size}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save OneOnOneNote ${note.id}")
                withContext(Dispatchers.Main) {
                    fragment.runOnActivity { fragment.somethingHappened(e, fragment.provideSurfaceView()) }
                }
            }
        }
    }
}
