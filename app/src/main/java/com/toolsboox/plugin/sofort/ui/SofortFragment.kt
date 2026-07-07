package com.toolsboox.plugin.sofort.ui

import android.graphics.Color
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.da.TextElement
import com.toolsboox.databinding.FragmentSofortBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.sofort.da.SofortNote
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SofortFragment @Inject constructor() : SurfaceFragment() {

    @Inject
    lateinit var presenter: SofortPresenter

    override val view = R.layout.fragment_sofort

    private lateinit var binding: FragmentSofortBinding

    private var noteId: UUID? = null
    private var note: SofortNote? = null

    override fun provideSurfaceView(): SurfaceView = binding.surfaceView

    override fun provideToolbarDrawing(): ToolbarDrawingBinding = binding.toolbarDrawing

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSofortBinding.bind(view)

        arguments?.getString("noteId")?.let { id ->
            runCatching { UUID.fromString(id) }.getOrNull()?.let { noteId = it }
        }

        initializeSurface(true)
    }

    override fun onResume() {
        super.onResume()

        binding.templateImageView.setImageBitmap(templateBitmap)
        templateCanvas.drawColor(Color.WHITE)

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.sofort_drawing_title))

        GlobalScope.launch(Dispatchers.Main) {
            noteId?.let { presenter.load(this@SofortFragment, it) }
        }
    }

    fun renderNote(loadedNote: SofortNote?) {
        if (loadedNote != null) {
            note = loadedNote
            toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), loadedNote.title)
            applyStrokes(Stroke.listDeepCopy(loadedNote.strokes), true)
        }
    }

    override fun onStrokeChanged(strokes: MutableList<Stroke>) {
        val current = note ?: return
        presenter.save(this, current, strokes.toList())
        note = current.copy(strokes = strokes.toList(), updatedAt = System.currentTimeMillis())
    }

    override fun onTextElementsChanged(textElements: MutableList<TextElement>) {
        // not used
    }

    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}
