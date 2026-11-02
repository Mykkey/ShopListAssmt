package com.mmgpt.shoplistgpt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mmgpt.shoplistgpt.Mode
import com.mmgpt.shoplistgpt.ShoppingItem
import com.mmgpt.shoplistgpt.databinding.ItemShoppingBinding
import com.mmgpt.shoplistgpt.databinding.ItemAddBinding

class ItemAdapter(
    private val onAction: (ShoppingItem, Action) -> Unit,
    private val mode: Mode = Mode.SHOP,
    private val dragStart: ((RecyclerView.ViewHolder) -> Unit)? = null,
    private val onAdd: ((Int, String) -> Unit)? = null,
    private val onRowDoubleTap: ((ShoppingItem) -> Unit)? = null
) : ListAdapter<ShoppingItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    enum class Action { CHECK, FLAG, EDIT, DELETE }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_ADD) {
            val binding = ItemAddBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AddViewHolder(binding)
        } else {
            val binding = ItemShoppingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(getItem(position))
        } else if (holder is AddViewHolder) {
            holder.bind(position)
        }
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isPlaceholder) VIEW_ADD else VIEW_ITEM

    inner class ItemViewHolder(
        private val binding: ItemShoppingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShoppingItem) {
            onRowDoubleTap?.let { callback ->
                binding.root.setOnRowDoubleTapListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) callback(getItem(position))
                }
            }
            binding.tvName.text = item.name
            binding.chkPurchased.setOnCheckedChangeListener(null)
            binding.chkPurchased.isChecked = item.isChecked
            binding.chkPurchased.setOnCheckedChangeListener { _, isChecked ->
                onAction(item.copy(isChecked = isChecked), Action.CHECK)

                if (isChecked) {
                    binding.root.alpha = 0.7f
                    binding.tvName.paintFlags =
                        binding.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    binding.root.alpha = 1f
                    binding.tvName.paintFlags =
                        binding.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }
            val iconRes = if (item.isFlagged) {
                com.mmgpt.shoplistgpt.R.drawable.ic_flag_filled_24
            } else {
                com.mmgpt.shoplistgpt.R.drawable.ic_flag_outline_24
            }
            binding.btnFlag.setImageResource(iconRes)

            if (mode == Mode.PLAN) {
                binding.btnFlag.visibility = View.GONE
                binding.dragHandle.visibility = View.VISIBLE
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnEdit.setOnClickListener { onAction(item, Action.EDIT) }
                binding.btnDelete.setOnClickListener { onAction(item, Action.DELETE) }
                binding.dragHandle.setOnTouchListener { _, _ ->
                    dragStart?.invoke(this@ItemViewHolder)
                    false
                }
            } else {
                binding.btnFlag.visibility = View.VISIBLE
                binding.dragHandle.visibility = View.GONE
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            }

            if (item.isHeading) {
                binding.chkPurchased.isEnabled = false
                // A disabled but clickable view still swallows touches, which
                // would stop double taps on this part of the row.
                binding.chkPurchased.isClickable = false
                binding.btnFlag.isEnabled = false
                binding.tvName.setTypeface(null, Typeface.BOLD)
                val color = ContextCompat.getColor(binding.root.context, com.mmgpt.shoplistgpt.R.color.list_item_text)
                binding.tvName.setTextColor(color)
                binding.root.strokeWidth = 0
            } else {
                binding.chkPurchased.isEnabled = true
                binding.chkPurchased.isClickable = true
                binding.btnFlag.isEnabled = mode == Mode.SHOP
                binding.tvName.setTypeface(null, Typeface.NORMAL)
                val color = ContextCompat.getColor(binding.root.context, com.mmgpt.shoplistgpt.R.color.list_item_text)
                binding.tvName.setTextColor(color)

                val context = binding.root.context
                val bgRes: Int
                val borderRes: Int
                if (mode == Mode.PLAN) {
                    bgRes = com.mmgpt.shoplistgpt.R.color.have_home_bg
                    borderRes = com.mmgpt.shoplistgpt.R.color.have_home_border
                } else {
                    bgRes = if (item.isStruck) com.mmgpt.shoplistgpt.R.color.have_home_bg else com.mmgpt.shoplistgpt.R.color.need_buy_bg
                    borderRes = if (item.isStruck) com.mmgpt.shoplistgpt.R.color.have_home_border else com.mmgpt.shoplistgpt.R.color.need_buy_border
                }
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, bgRes))
                binding.root.strokeColor = ContextCompat.getColor(context, borderRes)
                binding.root.strokeWidth = context.resources.getDimensionPixelSize(com.mmgpt.shoplistgpt.R.dimen.item_border_width)
            }
            if (mode == Mode.SHOP) {
                binding.btnFlag.setOnClickListener { onAction(item, Action.FLAG) }
            } else {
                binding.btnFlag.setOnClickListener(null)
            }

            if (item.isChecked) {
                binding.root.alpha = 0.7f
                binding.tvName.paintFlags = binding.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.root.alpha = 1f
                binding.tvName.paintFlags = binding.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    inner class AddViewHolder(
        private val binding: ItemAddBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.btnAdd.setOnClickListener {
                val text = binding.etAdd.text.toString().trim()
                if (text.isNotEmpty()) {
                    val pos = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
                    onAdd?.invoke(pos, text)
                    binding.etAdd.setText("")
                }
            }
        }
    }

    fun moveItem(from: Int, to: Int) {
        val current = currentList.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        submitList(current)
    }

    companion object {
        private const val VIEW_ITEM = 0
        private const val VIEW_ADD = 1
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShoppingItem>() {
            override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem) =
                oldItem == newItem
        }
    }
}
