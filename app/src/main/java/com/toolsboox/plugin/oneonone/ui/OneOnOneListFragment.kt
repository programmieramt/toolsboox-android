package com.toolsboox.plugin.oneonone.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toolsboox.R
import com.toolsboox.databinding.FragmentOneononeListBinding
import com.toolsboox.plugin.oneonone.da.OneOnOneNote
import com.toolsboox.ui.plugin.ScreenFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class OneOnOneListFragment @Inject constructor() : ScreenFragment() {

    @Inject
    lateinit var presenter: OneOnOneListPresenter

    override val view = R.layout.fragment_oneonone_list

    private lateinit var binding: FragmentOneononeListBinding

    private val adapter = NoteAdapter(
        onOpen = { note -> openNote(note) },
        onDelete = { note -> presenter.delete(this, note) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentOneononeListBinding.bind(view)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.fabNew.setOnClickListener {
            findNavController().navigate(R.id.action_to_oneonone_meta, bundleOf())
        }
    }

    override fun onResume() {
        super.onResume()
        toolbar.root.title = getString(R.string.drawer_title, getString(R.string.app_name), getString(R.string.oneonone_list_title))
        presenter.load(this)
    }

    fun showNotes(notes: List<OneOnOneNote>) {
        adapter.submitList(notes)
        binding.emptyText.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun showLoading() {
        binding.mainProgress.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.mainProgress.visibility = View.INVISIBLE
    }

    private fun openNote(note: OneOnOneNote) {
        findNavController().navigate(
            R.id.action_to_oneonone,
            bundleOf("noteId" to note.id.toString(), "noteTitle" to note.title)
        )
    }

    class NoteAdapter(
        private val onOpen: (OneOnOneNote) -> Unit,
        private val onDelete: (OneOnOneNote) -> Unit
    ) : RecyclerView.Adapter<NoteAdapter.VH>() {

        private val items = mutableListOf<OneOnOneNote>()
        private val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        fun submitList(list: List<OneOnOneNote>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_oneonone_note, parent, false)
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
