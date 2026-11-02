package com.mmgpt.shoplistgpt

import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Start page / Shop / Plan switching in [MainActivity]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityModeTest {

    private lateinit var activity: MainActivity
    private lateinit var viewModel: ShoppingListViewModel

    private val categories = listOf(
        ShoppingCategory(
            1, "Fruit",
            listOf(ShoppingItem(1, "Apple", isChecked = true), ShoppingItem(2, "Pear"))
        ),
        ShoppingCategory(2, "Dairy", listOf(ShoppingItem(3, "Milk", isChecked = true))),
    )

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().get()
        viewModel = ViewModelProvider(activity)[ShoppingListViewModel::class.java]
        viewModel.setCategories(categories)
        viewModel.setPlanDate(null)
    }

    private val pager get() = activity.findViewById<ViewPager2>(R.id.viewPager)
    private val adapter get() = pager.adapter as CategoryPagerAdapter
    private val startPage get() = activity.findViewById<View>(R.id.startPage)
    private val title get() = activity.supportActionBar?.title?.toString()

    private fun click(id: Int) {
        activity.findViewById<View>(id).performClick()
    }

    private fun home() {
        activity.onOptionsItemSelected(RoboMenuItem(R.id.action_home))
    }

    private fun struck() = viewModel.categories.value!!.flatMap { it.items }.map { it.isStruck }

    @Test
    fun startsOnTheStartPageWithAShopPager() {
        assertEquals(View.VISIBLE, startPage.visibility)
        assertEquals(View.GONE, pager.visibility)
        assertEquals(Mode.SHOP, adapter.mode)
        assertEquals(activity.getString(R.string.app_name), title)
    }

    @Test
    fun shop_showsPagerWithShopLayout() {
        click(R.id.btnShop)
        assertEquals(View.GONE, startPage.visibility)
        assertEquals(View.VISIBLE, pager.visibility)
        assertEquals(Mode.SHOP, adapter.mode)
        assertEquals(categories.size + 1, adapter.itemCount)
        assertEquals(activity.getString(R.string.shop), title)
    }

    @Test
    fun shop_titleIncludesThePlanDateWhenThereIsOne() {
        viewModel.setPlanDate(LocalDate.of(2026, 3, 5))
        click(R.id.btnShop)
        assertEquals(activity.getString(R.string.shop_with_date, "5 Mar"), title)
    }

    @Test
    fun plan_showsPagerWithPlanLayoutAndMarksEverythingStruck() {
        click(R.id.btnPlan)
        assertEquals(View.VISIBLE, pager.visibility)
        assertEquals(Mode.PLAN, adapter.mode)
        assertEquals(categories.size + 1, adapter.itemCount)
        assertEquals(listOf(true, true, true), struck())
        assertEquals(LocalDate.now(), viewModel.planDate.value)
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM"))
        assertEquals(activity.getString(R.string.plan_with_date, today), title)
    }

    @Test
    fun leavingPlan_restoresShopPagerAndStrikesOnlyCheckedItems() {
        click(R.id.btnPlan)
        home()
        assertEquals(View.VISIBLE, startPage.visibility)
        assertEquals(View.GONE, pager.visibility)
        assertEquals(Mode.SHOP, adapter.mode)
        assertEquals(listOf(true, false, true), struck())
        assertEquals(activity.getString(R.string.app_name), title)
    }

    @Test
    fun goingHomeFromShopLeavesItemsAndPagerAlone() {
        click(R.id.btnShop)
        val before = adapter
        home()
        assertEquals(View.VISIBLE, startPage.visibility)
        assertEquals(listOf(false, false, false), struck())
        assertTrue("shop adapter is reused, not rebuilt", before === adapter)
    }

    @Test
    fun repeatedSwitchingAlwaysGetsTheRightPager() {
        click(R.id.btnPlan); assertEquals(Mode.PLAN, adapter.mode)
        home(); assertEquals(Mode.SHOP, adapter.mode)
        click(R.id.btnShop); assertEquals(Mode.SHOP, adapter.mode)
        home(); click(R.id.btnPlan); assertEquals(Mode.PLAN, adapter.mode)
    }

    @Test
    fun pageCountFollowsCategoriesAddedAndDeletedInPlanMode() {
        click(R.id.btnPlan)
        viewModel.addCategory(0, "Bakery")
        assertEquals(4, adapter.itemCount)
        viewModel.deleteCategory(0)
        viewModel.deleteCategory(0)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun noCategories_shopAndPlanStillHaveOneListPage() {
        viewModel.clearAllCategories()
        click(R.id.btnShop)
        assertEquals(1, adapter.itemCount)
        home()
        click(R.id.btnPlan)
        assertEquals(1, adapter.itemCount)
        assertEquals(Mode.PLAN, adapter.mode)
    }
}
