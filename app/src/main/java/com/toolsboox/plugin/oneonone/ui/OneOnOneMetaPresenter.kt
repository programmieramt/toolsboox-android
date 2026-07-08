package com.toolsboox.plugin.oneonone.ui

import android.os.Environment
import com.toolsboox.plugin.oneonone.da.OneOnOneNote
import com.toolsboox.plugin.oneonone.fi.OneOnOneService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class OneOnOneMetaPresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var oneOnOneService: OneOnOneService

    fun loadExisting(fragment: OneOnOneMetaFragment, id: UUID) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
            val note = oneOnOneService.load(rootPath, id)
            withContext(Dispatchers.Main) {
                if (note != null) fragment.populateForm(note)
            }
        }
    }

    fun save(fragment: OneOnOneMetaFragment, id: UUID?, title: String) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.showLoading() }
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val noteId = id ?: UUID.randomUUID()
                val existing = if (id != null) oneOnOneService.load(rootPath, id) else null
                val note = OneOnOneNote(
                    id = noteId,
                    title = title,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    strokes = existing?.strokes ?: emptyList()
                )
                oneOnOneService.save(rootPath, note)
                withContext(Dispatchers.Main) { fragment.navigateToDrawing(noteId, title) }
            } finally {
                withContext(Dispatchers.Main) { fragment.hideLoading() }
            }
        }
    }
}
