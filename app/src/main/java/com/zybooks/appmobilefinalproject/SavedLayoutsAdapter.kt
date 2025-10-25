package com.zybooks.appmobilefinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class SavedLayoutsAdapter(
    private var layouts: List<SavedLayout>,
    private val onItemClick: (SavedLayout) -> Unit,
    private val onLongClick: (SavedLayout, View) -> Unit
) : RecyclerView.Adapter<SavedLayoutsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chipTitle: Chip = view.findViewById(R.id.chipTitle)
        val imgPreview: ImageView = view.findViewById(R.id.imgPreview)
        val cardPreview: MaterialCardView = view.findViewById(R.id.cardPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_panel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val layout = layouts[position]

        // Set the title in the chip
        holder.chipTitle.text = layout.name

        // Set preview image if available
        if (layout.thumbnailResId != null) {
            holder.imgPreview.setImageResource(layout.thumbnailResId)
        } else {
            // Use default placeholder
            holder.imgPreview.setImageResource(R.drawable.outline_design_services_24)
        }

        // Click on the entire card to open the layout
        holder.cardPreview.setOnClickListener {
            onItemClick(layout)
        }

        // Long press for more options
        holder.cardPreview.setOnLongClickListener {
            onLongClick(layout, it)
            true
        }

        // Also make chip clickable
        holder.chipTitle.setOnClickListener {
            onItemClick(layout)
        }
    }

    override fun getItemCount() = layouts.size

    fun updateData(newLayouts: List<SavedLayout>) {
        layouts = newLayouts
        notifyDataSetChanged()
    }
}