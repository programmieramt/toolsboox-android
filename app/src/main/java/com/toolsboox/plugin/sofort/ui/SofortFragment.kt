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
import androidx.navigation.fragment.findNavController
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

    override fun toolbarCollapsedKey(): String = "sofortToolbarCollapsed"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSofortBinding.bind(view)

        arguments?.let { args ->
            args.getString("noteId")?.let { idStr ->
                runCatching { UUID.fromString(idStr) }.getOrNull()?.let { uuid ->
                    noteId = uuid
                    // Initialize note synchronously so onStrokeChanged can save
                    // even before the async file load completes.
                    val title = args.getString("noteTitle") ?: ""
                    if (note == null) note = SofortNote(id = uuid, title = title)
                }
            }
        }

        initializeSurface(true)

        binding.surfaceView.setOnHoverListener { _, motionEvent ->
            callback(motionEvent, true)
        }
        binding.surfaceView.setOnTouchListener { _, motionEvent ->
            if (callback(motionEvent, false)) return@setOnTouchListener true
            handleZoomPanTouch(motionEvent)
        }
    }

    override fun onResume() {
        super.onResume()

        binding.templateImageView.setImageBitmap(templateBitmap)
        templateCanvas.drawColor(Color.WHITE)

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.sofort_drawing_title))

        val navigateBack = {
            if (!findNavController().popBackStack(R.id.SofortListFragment, false)) {
                findNavController().navigateUp()
            }
        }

        provideToolbarDrawing().toolbarSwipeUp.setOnClickListener { navigateBack() }

        binding.fabSave.setOnClickListener {
            // note.strokes is always current (updated by onStrokeChanged after every stroke)
            note?.let { n -> presenter.save(this, n, n.strokes) }
            navigateBack()
        }

        noteId?.let { presenter.load(this, it) }
    }

    fun renderNote(loadedNote: SofortNote?) {
        if (loadedNote == null) return
        // Only apply loaded strokes if the user hasn't already drawn something
        // (avoids overwriting a fresh drawing with old data in the rare race window).
        val freshStrokes = note?.strokes ?: emptyList()
        note = if (freshStrokes.isEmpty()) loadedNote else loadedNote.copy(strokes = freshStrokes)
        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), loadedNote.title)
        applyStrokes(Stroke.listDeepCopy(note!!.strokes), true)
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
