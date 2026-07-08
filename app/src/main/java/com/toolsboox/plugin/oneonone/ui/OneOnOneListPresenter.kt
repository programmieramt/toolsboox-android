package com.toolsboox.plugin.oneonone.ui

import android.os.Environment
import com.toolsboox.plugin.oneonone.da.OneOnOneNote
import com.toolsboox.plugin.oneonone.fi.OneOnOneService
import com.toolsboox.ui.plugin.FragmentPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OneOnOneListPresenter @Inject constructor() : FragmentPresenter() {

    @Inject
    lateinit var oneOnOneService: OneOnOneService

    fun load(fragment: OneOnOneListFragment) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { fragment.showLoading() }
                val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
                val notes = oneOnOneService.list(rootPath)
                withContext(Dispatchers.Main) { fragment.showNotes(notes) }
            } finally {
                withContext(Dispatchers.Main) { fragment.hideLoading() }
            }
        }
    }

    fun delete(fragment: OneOnOneListFragment, note: OneOnOneNote) {
        if (!checkPermissions(fragment, fragment.requireView())) return

        GlobalScope.launch(Dispatchers.IO) {
            val rootPath = rootPath(fragment, Environment.DIRECTORY_DOCUMENTS)
            oneOnOneService.delete(rootPath, note.id)
            val notes = oneOnOneService.list(rootPath)
            withContext(Dispatchers.Main) { fragment.showNotes(notes) }
        }
    }
}
