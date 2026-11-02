package com.mmgpt.shoplistgpt.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mmgpt.shoplistgpt.ShoppingCategory
import com.mmgpt.shoplistgpt.databinding.ItemCategorySummaryBinding

class CategorySummaryAdapter(
    private val onRowDoubleTap: ((ShoppingCategory) -> Unit)? = null
) : ListAdapter<ShoppingCategory, CategorySummaryAdapter.ViewHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategorySummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding).also { holder ->
            onRowDoubleTap?.let { callback ->
                holder.itemView.setOnRowDoubleTapListener {
                    val position = holder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) callback(getItem(position))
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    class ViewHolder(private val binding: ItemCategorySummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: ShoppingCategory) {
            binding.tvName.text = category.heading
            binding.tvCount.text = "${category.remainingCount}/${category.totalCount}"
            binding.tvFlagged.text = category.flaggedCount.toString()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShoppingCategory>() {
            override fun areItemsTheSame(oldItem: ShoppingCategory, newItem: ShoppingCategory) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ShoppingCategory, newItem: ShoppingCategory) = oldItem == newItem
        }
    }
}
