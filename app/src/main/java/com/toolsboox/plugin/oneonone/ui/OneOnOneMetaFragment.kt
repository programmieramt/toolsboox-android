package com.toolsboox.plugin.oneonone.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.toolsboox.R
import com.toolsboox.databinding.FragmentOneononeMetaBinding
import com.toolsboox.plugin.oneonone.da.OneOnOneNote
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class OneOnOneMetaFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var presenter: OneOnOneMetaPresenter

    override val view = R.layout.fragment_oneonone_meta

    private lateinit var binding: FragmentOneononeMetaBinding

    private var noteId: UUID? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentOneononeMetaBinding.bind(view)

        arguments?.getString("noteId")?.let { id ->
            runCatching { UUID.fromString(id) }.getOrNull()?.let { uuid ->
                noteId = uuid
                presenter.loadExisting(this, uuid)
            }
        }

        binding.btnWeiter.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isEmpty()) {
                binding.etTitle.error = getString(R.string.validation_field_required)
                return@setOnClickListener
            }
            presenter.save(this, noteId, title)
        }
    }

    override fun onResume() {
        super.onResume()
        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.oneonone_meta_title))
    }

    fun populateForm(note: OneOnOneNote) {
        binding.etTitle.setText(note.title)
    }

    fun navigateToDrawing(id: UUID, title: String) {
        findNavController().navigate(
            R.id.action_to_oneonone,
            bundleOf("noteId" to id.toString(), "noteTitle" to title)
        )
    }

    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}
