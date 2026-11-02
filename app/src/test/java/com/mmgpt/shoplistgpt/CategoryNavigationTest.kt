package com.mmgpt.shoplistgpt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryNavigationTest {

    private val categories = listOf(
        ShoppingCategory(10, "Fruit", emptyList()),
        ShoppingCategory(3, "Dairy", emptyList()),
        ShoppingCategory(7, "Bakery", emptyList()),
    )

    @Test
    fun shopPage_isTheCategoryIndex() {
        assertEquals(0, CategoryNavigation.pageFor(Mode.SHOP, categories, 10))
        assertEquals(1, CategoryNavigation.pageFor(Mode.SHOP, categories, 3))
        assertEquals(2, CategoryNavigation.pageFor(Mode.SHOP, categories, 7))
    }

    @Test
    fun planPage_isOffsetByTheCategoryListPage() {
        assertEquals(1, CategoryNavigation.pageFor(Mode.PLAN, categories, 10))
        assertEquals(2, CategoryNavigation.pageFor(Mode.PLAN, categories, 3))
        assertEquals(3, CategoryNavigation.pageFor(Mode.PLAN, categories, 7))
    }

    @Test
    fun lookupFollowsTheCategoryWhenTheyAreReordered() {
        val reordered = listOf(categories[2], categories[0], categories[1])
        assertEquals(0, CategoryNavigation.pageFor(Mode.SHOP, reordered, 7))
        assertEquals(1, CategoryNavigation.pageFor(Mode.PLAN, reordered, 7))
        assertEquals(2, CategoryNavigation.pageFor(Mode.SHOP, reordered, 3))
    }

    @Test
    fun unknownCategoryOrMissingListGivesNull() {
        assertNull(CategoryNavigation.pageFor(Mode.SHOP, categories, 999))
        assertNull(CategoryNavigation.pageFor(Mode.PLAN, categories, 999))
        assertNull(CategoryNavigation.pageFor(Mode.SHOP, null, 10))
        assertNull(CategoryNavigation.pageFor(Mode.PLAN, emptyList(), 10))
    }
}
