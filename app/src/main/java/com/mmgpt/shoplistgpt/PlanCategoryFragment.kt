package com.mmgpt.shoplistgpt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mmgpt.shoplistgpt.adapter.ItemAdapter
import com.mmgpt.shoplistgpt.databinding.FragmentCategoryBinding
import com.mmgpt.shoplistgpt.ShoppingItem

class PlanCategoryFragment : Fragment() {

    private val viewModel: ShoppingListViewModel by activityViewModels()
    private var index: Int = 0
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = arguments?.getInt(ARG_INDEX) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chkSelectAll.visibility = View.VISIBLE
        lateinit var adapter: ItemAdapter
        val dragHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                val itemFrom = adapter.currentList.getOrNull(from)
                val itemTo = adapter.currentList.getOrNull(to)
                if (itemFrom?.isPlaceholder == true || itemTo?.isPlaceholder == true) return false
                viewModel.reorderItem(index, from - 1, to - 1)
                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = false
        })

        adapter = ItemAdapter(
            onAction = { item, action ->
                when (action) {
                    ItemAdapter.Action.CHECK -> viewModel.checkItem(item)
                    ItemAdapter.Action.EDIT -> showEditDialog(item)
                    ItemAdapter.Action.DELETE -> viewModel.deleteItem(item)
                    else -> {}
                }
            },
            mode = Mode.PLAN,
            dragStart = { vh -> dragHelper.startDrag(vh) },
            onAdd = { position, name ->
                viewModel.addItem(index, position - 1, name)
            }
        )
        binding.recyclerView.adapter = adapter
        dragHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.categories.observe(viewLifecycleOwner) { list ->
            val cat = list.getOrNull(index)
            if (cat != null) {
                binding.tvHeading.text = cat.heading
                binding.tvCount.text = ""

                val allChecked = cat.items.filterNot { it.isHeading }.all { it.isChecked }
                binding.chkSelectAll.setOnCheckedChangeListener(null)
                binding.chkSelectAll.isChecked = allChecked
                binding.chkSelectAll.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.setAllChecked(index, isChecked)
                }

                val items = mutableListOf<ShoppingItem>()
                items.add(ShoppingItem(-1, "", isPlaceholder = true))
                items.addAll(cat.items)
                items.add(ShoppingItem(-2, "", isPlaceholder = true))
                adapter.submitList(items)
            }
        }
    }

    private fun showEditDialog(item: ShoppingItem) {
        val input = android.widget.EditText(requireContext()).apply { setText(item.name) }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    viewModel.editItem(item, text)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INDEX = "index"
        fun newInstance(index: Int): PlanCategoryFragment {
            val fragment = PlanCategoryFragment()
            fragment.arguments = Bundle().apply { putInt(ARG_INDEX, index) }
            return fragment
        }
    }
}
