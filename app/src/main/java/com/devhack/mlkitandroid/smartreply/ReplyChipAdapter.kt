package com.devhack.mlkitandroid.smartreply

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devhack.mlkitandroid.R
import com.google.mlkit.nl.smartreply.SmartReplySuggestion
import java.util.*

class ReplyChipAdapter(private val listener: ClickListener) :
    RecyclerView.Adapter<ReplyChipAdapter.ViewHolder>() {

    private val suggestions = ArrayList<SmartReplySuggestion>()

    interface ClickListener {
        fun onChipClick(chipText: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.smart_reply_chip, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.bind(suggestion)
    }

    override fun getItemCount(): Int = suggestions.size

    fun setSuggestions(suggestions: List<SmartReplySuggestion>) {
        this.suggestions.clear()
        this.suggestions.addAll(suggestions)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val text: TextView = itemView.findViewById(R.id.smartReplyText)

        fun bind(suggestion: SmartReplySuggestion) {
            text.text = suggestion.text
            itemView.setOnClickListener { listener.onChipClick(suggestion.text) }
        }
    }
}