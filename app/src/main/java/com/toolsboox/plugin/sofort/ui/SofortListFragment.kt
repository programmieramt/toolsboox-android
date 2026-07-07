package com.toolsboox.plugin.sofort.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toolsboox.R
import com.toolsboox.databinding.FragmentSofortListBinding
import com.toolsboox.plugin.sofort.da.SofortNote
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SofortListFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var presenter: SofortListPresenter

    override val view = R.layout.fragment_sofort_list

    private lateinit var binding: FragmentSofortListBinding

    private val adapter = NoteAdapter(
        onOpen = { note -> openNote(note) },
        onDelete = { note -> presenter.delete(this, note) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSofortListBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.fabNew.setOnClickListener {
            findNavController().navigate(R.id.action_to_sofort_meta, bundleOf())
        }
    }

    override fun onResume() {
        super.onResume()
        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.sofort_list_title))
        presenter.load(this)
    }

    fun showNotes(notes: List<SofortNote>) {
        adapter.submitList(notes)
        binding.emptyText.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }

    private fun openNote(note: SofortNote) {
        findNavController().navigate(
            R.id.action_to_sofort,
            bundleOf("noteId" to note.id.toString())
        )
    }

    class NoteAdapter(
        private val onOpen: (SofortNote) -> Unit,
        private val onDelete: (SofortNote) -> Unit
    ) : RecyclerView.Adapter<NoteAdapter.VH>() {

        private val items = mutableListOf<SofortNote>()
        private val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        fun submitList(list: List<SofortNote>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val inflater = LayoutInflater.from(parent.context)
            val v = inflater.inflate(R.layout.item_sofort_note, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val note = items[position]
            holder.title.text = note.title
            holder.date.text = fmt.format(Date(note.updatedAt))
            holder.itemView.setOnClickListener { onOpen(note) }
            holder.itemView.setOnLongClickListener { onDelete(note); true }
        }

        override fun getItemCount() = items.size

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.noteTitle)
            val date: TextView = v.findViewById(R.id.noteDate)
        }
    }
}
