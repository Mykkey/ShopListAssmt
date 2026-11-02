package com.mmgpt.shoplistgpt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageLayoutTest {

    @Test
    fun shop_listIsTheLastPage() {
        val layout = PageLayout(Mode.SHOP, 3)
        assertEquals(4, layout.pageCount)
        assertEquals(3, layout.listPage)
        assertEquals(listOf(0, 1, 2), (0..2).map { layout.pageOfCategory(it) })
        assertEquals(listOf(0, 1, 2, null), (0..3).map { layout.categoryIndexAt(it) })
    }

    @Test
    fun plan_listIsTheFirstPage() {
        val layout = PageLayout(Mode.PLAN, 3)
        assertEquals(4, layout.pageCount)
        assertEquals(0, layout.listPage)
        assertEquals(listOf(1, 2, 3), (0..2).map { layout.pageOfCategory(it) })
        assertEquals(listOf(null, 0, 1, 2), (0..3).map { layout.categoryIndexAt(it) })
    }

    @Test
    fun pageAndCategoryIndexAreInverses() {
        for (mode in Mode.values()) {
            for (count in 0..6) {
                val layout = PageLayout(mode, count)
                for (i in 0 until count) {
                    assertEquals("$mode/$count/$i", i, layout.categoryIndexAt(layout.pageOfCategory(i)))
                }
                // Every page is either a category or the list, never both.
                for (page in 0 until layout.pageCount) {
                    assertEquals(layout.isListPage(page), layout.categoryIndexAt(page) == null)
                }
            }
        }
    }

    @Test
    fun noCategories_onlyTheListPageExists() {
        for (mode in Mode.values()) {
            val layout = PageLayout(mode, 0)
            assertEquals(1, layout.pageCount)
            assertTrue(layout.isListPage(0))
            assertNull(layout.categoryIndexAt(0))
        }
    }

    @Test
    fun outOfRangePagesAreNotCategories() {
        val layout = PageLayout(Mode.PLAN, 2)
        assertNull(layout.categoryIndexAt(-1))
        assertNull(layout.categoryIndexAt(3))
        assertFalse(layout.isListPage(1))
    }
}
