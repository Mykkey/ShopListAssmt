package com.mmgpt.shoplistgpt

/** The two ways the category pager can be arranged. */
enum class Mode { SHOP, PLAN }

/**
 * Where each page of the category pager lives, for one [mode] and a given
 * number of categories. This is the single place that knows the arithmetic:
 *
 * - Shop: pages 0..n-1 are the categories, page n is the summary list.
 * - Plan: page 0 is the category list, pages 1..n are the categories.
 */
data class PageLayout(val mode: Mode, val categoryCount: Int) {

    val pageCount: Int get() = categoryCount + 1

    val listPage: Int get() = if (mode == Mode.PLAN) 0 else categoryCount

    fun isListPage(page: Int): Boolean = page == listPage

    /** Page showing the category at [categoryIndex]. */
    fun pageOfCategory(categoryIndex: Int): Int =
        if (mode == Mode.PLAN) categoryIndex + 1 else categoryIndex

    /** Index of the category shown on [page], or null for the list page. */
    fun categoryIndexAt(page: Int): Int? = when {
        isListPage(page) || page !in 0 until pageCount -> null
        mode == Mode.PLAN -> page - 1
        else -> page
    }
}
