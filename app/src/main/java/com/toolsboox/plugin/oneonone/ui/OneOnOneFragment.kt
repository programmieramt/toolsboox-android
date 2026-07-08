package com.toolsboox.plugin.oneonone.ui

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import androidx.navigation.fragment.findNavController
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.da.TextElement
import com.toolsboox.databinding.FragmentOneononeBinding
import com.toolsboox.databinding.ToolbarDrawingBinding
import com.toolsboox.plugin.oneonone.da.OneOnOneNote
import com.toolsboox.ui.plugin.SurfaceFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class OneOnOneFragment @Inject constructor() : SurfaceFragment() {

    @Inject
    lateinit var presenter: OneOnOnePresenter

    override val view = R.layout.fragment_oneonone

    private lateinit var binding: FragmentOneononeBinding

    private var noteId: UUID? = null
    private var note: OneOnOneNote? = null

    override fun provideSurfaceView(): SurfaceView = binding.surfaceView

    override fun provideToolbarDrawing(): ToolbarDrawingBinding = binding.toolbarDrawing

    override fun toolbarCollapsedKey(): String = "oneononeToolbarCollapsed"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentOneononeBinding.bind(view)

        arguments?.let { args ->
            args.getString("noteId")?.let { idStr ->
                runCatching { UUID.fromString(idStr) }.getOrNull()?.let { uuid ->
                    noteId = uuid
                    val title = args.getString("noteTitle") ?: ""
                    if (note == null) note = OneOnOneNote(id = uuid, title = title)
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
        drawTemplate()

        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.oneonone_drawing_title))

        val navigateBack = {
            if (!findNavController().popBackStack(R.id.OneOnOneListFragment, false)) {
                findNavController().navigateUp()
            }
        }

        provideToolbarDrawing().toolbarSwipeUp.setOnClickListener { navigateBack() }

        binding.fabSave.setOnClickListener {
            note?.let { n -> presenter.save(this, n, n.strokes) }
            navigateBack()
        }

        noteId?.let { presenter.load(this, it) }
    }

    fun renderNote(loadedNote: OneOnOneNote?) {
        if (loadedNote == null) return
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

    override fun onTextElementsChanged(textElements: MutableList<TextElement>) {}

    private fun drawTemplate() {
        val w = templateBitmap.width.toFloat()
        val h = templateBitmap.height.toFloat()
        val accent = Color.argb(160, 90, 80, 180)   // semi-transparent purple

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
        }
        val sectionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Top reminder bar with the 4-step process
        val barH = h * 0.048f
        templateCanvas.drawRect(0f, 0f, w, barH, headerPaint)
        val steps = listOf("1. On your mind?", "2. What else?", "3. Real problem?", "4. How can I help?")
        val stepW = w / steps.size
        steps.forEachIndexed { i, text ->
            templateCanvas.drawText(text, i * stepW + 16f, barH * 0.72f, headerTextPaint)
        }

        // 5 section headings, evenly distributed below the bar
        val sections = listOf(
            "THEMA / WAS AUF DEM HERZEN LIEGT",
            "DAS ECHTE PROBLEM / KERN",
            "WIE KANN ICH HELFEN / VEREINBARUNGEN",
            "MEIN EINDRUCK / STIMMUNG",
            "OFFENE PUNKTE FUER NAECHSTES 1:1"
        )
        val sectionH = (h - barH) / sections.size
        sections.forEachIndexed { i, label ->
            val labelY = barH + i * sectionH + sectionH * 0.12f
            templateCanvas.drawText(label, 16f, labelY + sectionTextPaint.textSize, sectionTextPaint)
            val textEnd = 16f + sectionTextPaint.measureText(label) + 24f
            templateCanvas.drawLine(textEnd, labelY + sectionTextPaint.textSize * 0.6f, w - 16f, labelY + sectionTextPaint.textSize * 0.6f, rulePaint)
        }
    }

    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }
}
