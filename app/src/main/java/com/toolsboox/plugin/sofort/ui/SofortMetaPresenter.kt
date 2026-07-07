package com.toolsboox.plugin.sofort.ui

import android.os.Environment
import com.toolsboox.plugin.sofort.da.SofortNote
import com.toolsboox.plugin.sofort.fi.SofortService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class SofortMetaPresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var sofortService: SofortService

    fun loadExisting(fragment: SofortMetaFragment, id: UUID) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
            val note = sofortService.load(rootPath, id)
            withContext(Dispatchers.Main) {
                if (note != null) fragment.populateForm(note)
            }
        }
    }

    fun save(fragment: SofortMetaFragment, id: UUID?, title: String) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.showLoading() }
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)

                val noteId = id ?: UUID.randomUUID()
                val existing = if (id != null) sofortService.load(rootPath, id) else null
                val note = SofortNote(
                    id = noteId,
                    title = title,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    strokes = existing?.strokes ?: emptyList()
                )
                sofortService.save(rootPath, note)
                withContext(Dispatchers.Main) { fragment.navigateToDrawing(noteId) }
            } finally {
                withContext(Dispatchers.Main) { fragment.hideLoading() }
            }
        }
    }
}
