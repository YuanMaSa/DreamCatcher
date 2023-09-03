package edu.vt.cs5254.dreamcatcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding


const val CONCEIVED_COLOR = "#FF95CA"
const val REFLECTION_COLOR = "#6C3365"
const val DEFERRED_COLOR = "#c2e7ff"
const val FULFILLED_COLOR = "#FFF8D7"


class DreamEntryHolder(private val binding: ListItemDreamEntryBinding) :
    RecyclerView.ViewHolder(binding.root) {
    lateinit var boundEntry: DreamEntry
        private set

    fun bind(dreamEntry: DreamEntry) {
        boundEntry = dreamEntry
        binding.dreamEntryButton.displayEntry(dreamEntry)
    }

    private fun Button.displayEntry(entry: DreamEntry) {
        visibility = View.VISIBLE
        isEnabled = false
        text = entry.kind.toString()

        when (entry.kind) {
            DreamEntryKind.CONCEIVED -> {
                setBackgroundWithContrastingText(CONCEIVED_COLOR)
            }
            DreamEntryKind.REFLECTION -> {
                isAllCaps = false
                text = entry.text
                setBackgroundWithContrastingText(REFLECTION_COLOR)
            }
            DreamEntryKind.DEFERRED -> {
                setBackgroundWithContrastingText(DEFERRED_COLOR)
            }
            DreamEntryKind.FULFILLED -> {
                setBackgroundWithContrastingText(FULFILLED_COLOR)
            }
        }
    }

}

class DreamEntryAdapter(private val entries: List<DreamEntry>) :
    RecyclerView.Adapter<DreamEntryHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamEntryHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemDreamEntryBinding.inflate(inflater, parent, false)
        return DreamEntryHolder(binding)
    }

    override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int {
        return entries.size
    }

}