package com.mmgpt.shoplistgpt

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CategoryPagerAdapter(
    activity: FragmentActivity,
    private val viewModel: ShoppingListViewModel,
    val mode: Mode
) : FragmentStateAdapter(activity) {

    private val layout: PageLayout
        get() = PageLayout(mode, viewModel.categories.value?.size ?: 0)

    override fun getItemCount(): Int = layout.pageCount

    override fun createFragment(position: Int): Fragment {
        val layout = layout
        val index = layout.categoryIndexAt(position)
        return when {
            index == null && mode == Mode.PLAN -> PlanCategoriesFragment()
            index == null -> ShopCategoriesFragment()
            mode == Mode.PLAN -> PlanCategoryFragment.newInstance(index)
            else -> CategoryFragment.newInstance(index)
        }
    }
}
