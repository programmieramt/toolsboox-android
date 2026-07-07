package com.toolsboox.plugin.sofort.ui

import android.os.Environment
import com.toolsboox.plugin.sofort.da.SofortNote
import com.toolsboox.plugin.sofort.fi.SofortService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SofortListPresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var sofortService: SofortService

    fun load(fragment: SofortListFragment) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.showLoading() }
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val notes = sofortService.list(rootPath)
                withContext(Dispatchers.Main) { fragment.showNotes(notes) }
            } finally {
                withContext(Dispatchers.Main) { fragment.hideLoading() }
            }
        }
    }

    fun delete(fragment: SofortListFragment, note: SofortNote) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
            sofortService.delete(rootPath, note.id)
            val notes = sofortService.list(rootPath)
            withContext(Dispatchers.Main) { fragment.showNotes(notes) }
        }
    }
}
