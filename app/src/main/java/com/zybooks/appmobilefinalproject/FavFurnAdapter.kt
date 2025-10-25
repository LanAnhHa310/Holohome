package com.zybooks.appmobilefinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zybooks.appmobilefinalproject.FavoriteFurn.FavFurnitureItem

class FavFurnAdapter :
    ListAdapter<FavFurnitureItem, FavFurnAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FavFurnitureItem>() {
            override fun areItemsTheSame(
                oldItem: FavFurnitureItem,
                newItem: FavFurnitureItem
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: FavFurnitureItem,
                newItem: FavFurnitureItem
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
        holder.img.setImageResource(item.imageRes)

        holder.itemView.setOnClickListener {
            // TODO: handle item click (e.g., open detail / toggle favorite)
        }
    }
}
