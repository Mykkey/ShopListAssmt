package com.mmgpt.shoplistgpt

/**
 * Finds the pager page that shows a category.
 *
 * The lookup is by category id (not by the position of the row that was
 * tapped) so it stays correct however the rows are ordered or animated.
 */
object CategoryNavigation {

    fun pageFor(mode: Mode, categories: List<ShoppingCategory>?, categoryId: Long): Int? {
        if (categories == null) return null
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index < 0) return null
        return PageLayout(mode, categories.size).pageOfCategory(index)
    }
}
