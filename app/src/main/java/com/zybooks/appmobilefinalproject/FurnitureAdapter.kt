package com.zybooks.appmobilefinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// to load the image from url
import coil.load

class FurnitureAdapter :
    ListAdapter<MainActivity.FurnitureItem, FurnitureAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MainActivity.FurnitureItem>() {
            override fun areItemsTheSame(
                oldItem: MainActivity.FurnitureItem,
                newItem: MainActivity.FurnitureItem
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: MainActivity.FurnitureItem,
                newItem: MainActivity.FurnitureItem
            ) = oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.img)
        val title: TextView = view.findViewById(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_furniture, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.title.text = item.name
        holder.img.load(item.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.placeholdertile)   // shown while loading
            error(R.drawable.placeholdertile)         // shown if URL fails
        }

        holder.itemView.setOnClickListener {
            // TODO: handle item click (e.g., place in AR view)
        }
    }
}
