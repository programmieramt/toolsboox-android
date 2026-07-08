package com.toolsboox.plugin.sofort.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.toolsboox.R
import com.toolsboox.databinding.FragmentSofortMetaBinding
import com.toolsboox.plugin.sofort.da.SofortNote
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SofortMetaFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var presenter: SofortMetaPresenter

    override val view = R.layout.fragment_sofort_meta

    private lateinit var binding: FragmentSofortMetaBinding

    private var noteId: UUID? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSofortMetaBinding.bind(view)

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
        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.sofort_meta_title))
    }

    fun populateForm(note: SofortNote) {
        binding.etTitle.setText(note.title)
    }

    fun navigateToDrawing(id: UUID, title: String) {
        findNavController().navigate(
            R.id.action_to_sofort,
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
