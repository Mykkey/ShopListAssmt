package com.mmgpt.shoplistgpt

import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mmgpt.shoplistgpt.adapter.CategorySummaryAdapter
import com.mmgpt.shoplistgpt.adapter.ItemAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Double tapping a category row (Plan mode category list, Shop mode summary)
 * must report exactly the category that was tapped, wherever on the row the
 * tap lands and however the rows have been reordered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryDoubleTapTest {

    private lateinit var activity: AppCompatActivity
    private lateinit var recyclerView: RecyclerView

    private val categories = listOf(
        ShoppingCategory(10, "Fruit", emptyList()),
        ShoppingCategory(3, "Dairy", emptyList()),
        ShoppingCategory(7, "Bakery", emptyList()),
    )

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        recyclerView = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            id = View.generateViewId()
        }
        activity.setContentView(recyclerView)
    }

    // ---- Plan mode: ItemAdapter showing [placeholder, headings..., placeholder] ----

    private fun planRows(list: List<ShoppingCategory>): List<ShoppingItem> =
        listOf(ShoppingItem(-1, "", isPlaceholder = true)) +
            list.map { ShoppingItem(it.id, it.heading, isHeading = true) } +
            listOf(ShoppingItem(-2, "", isPlaceholder = true))

    private fun planAdapter(tapped: MutableList<ShoppingItem>, actions: MutableList<ItemAdapter.Action> = mutableListOf()) =
        ItemAdapter(
            onAction = { _, action -> actions.add(action) },
            mode = Mode.PLAN,
            onRowDoubleTap = { tapped.add(it) }
        ).also {
            recyclerView.adapter = it
            it.submitList(planRows(categories))
            layOut()
        }

    @Test
    fun plan_doubleTapOnNameReportsThatCategory() {
        val tapped = mutableListOf<ShoppingItem>()
        planAdapter(tapped)
        categories.forEachIndexed { i, cat ->
            doubleTap(rowView(i + 1, R.id.tvName))
            assertEquals(cat.id, tapped.last().id)
        }
        assertEquals(listOf(10L, 3L, 7L), tapped.map { it.id })
    }

    @Test
    fun plan_doubleTapOnDisabledCheckboxAreaStillCounts() {
        val tapped = mutableListOf<ShoppingItem>()
        planAdapter(tapped)
        doubleTap(rowView(2, R.id.chkPurchased))
        assertEquals(listOf(3L), tapped.map { it.id })
    }

    @Test
    fun plan_doubleTapOnRowPaddingCounts() {
        val tapped = mutableListOf<ShoppingItem>()
        planAdapter(tapped)
        val row = recyclerView.getChildAt(3)
        // 2dp inside the card's top-left corner: padding, no child view there.
        doubleTapAt(row, 2f, 2f)
        assertEquals(listOf(7L), tapped.map { it.id })
    }

    @Test
    fun plan_singleTapDoesNothing() {
        val tapped = mutableListOf<ShoppingItem>()
        planAdapter(tapped)
        tap(rowView(1, R.id.tvName), downTime = SystemClock.uptimeMillis())
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(tapped.isEmpty())
    }

    @Test
    fun plan_twoSlowTapsAreNotADoubleTap() {
        val tapped = mutableListOf<ShoppingItem>()
        planAdapter(tapped)
        val v = rowView(1, R.id.tvName)
        val t = SystemClock.uptimeMillis()
        tap(v, downTime = t)
        tap(v, downTime = t + 1500)
        assertTrue(tapped.isEmpty())
    }

    @Test
    fun plan_tapsOnDifferentRowsAreNotADoubleTap() {
        val tapped = mutableListOf<ShoppingItem>()
        planAdapter(tapped)
        val t = SystemClock.uptimeMillis()
        tap(rowView(1, R.id.tvName), downTime = t)
        tap(rowView(2, R.id.tvName), downTime = t + 120)
        assertTrue(tapped.isEmpty())
    }

    @Test
    fun plan_placeholderRowsNeverNavigate() {
        val tapped = mutableListOf<ShoppingItem>()
        planAdapter(tapped)
        doubleTapAt(recyclerView.getChildAt(0), 5f, 5f)
        doubleTapAt(recyclerView.getChildAt(recyclerView.childCount - 1), 5f, 5f)
        assertTrue(tapped.isEmpty())
    }

    @Test
    fun plan_editButtonKeepsItsOwnBehaviourAndDoesNotNavigate() {
        val tapped = mutableListOf<ShoppingItem>()
        val actions = mutableListOf<ItemAdapter.Action>()
        planAdapter(tapped, actions)
        val edit = rowView(1, R.id.btnEdit)
        val t = SystemClock.uptimeMillis()
        tap(edit, downTime = t)
        // Clicks inside a scrolling container fire after the tap timeout.
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300))
        assertEquals(listOf(ItemAdapter.Action.EDIT), actions)
        assertTrue(tapped.isEmpty())
    }

    @Test
    fun plan_reportsTheCategoryNowInTheRowAfterAReorder() {
        val tapped = mutableListOf<ShoppingItem>()
        val adapter = planAdapter(tapped)
        // Row 1 held Fruit; after the move it holds Bakery.
        val reordered = listOf(categories[2], categories[0], categories[1])
        adapter.submitList(planRows(reordered))
        waitUntil { adapter.currentList[1].id == 7L }
        layOut()
        doubleTap(rowView(1, R.id.tvName))
        assertEquals(listOf(7L), tapped.map { it.id })
    }

    /**
     * Documents the original defect: a GestureDetector fed from a touch listener
     * on the RecyclerView never sees taps that land on a child that consumes
     * touches (here the greyed-out checkbox of a heading row).
     */
    @Test
    fun legacy_recyclerViewLevelDetectorMissesTapsOnConsumingChildren() {
        planAdapterWithoutRowListener()
        var doubleTaps = 0
        val detector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                doubleTaps++
                return true
            }
        })
        recyclerView.setOnTouchListener { _, e -> detector.onTouchEvent(e); false }

        // Greyed-out checkbox, with the pre-fix "clickable" checkbox restored.
        val checkbox = rowView(1, R.id.chkPurchased)
        checkbox.isClickable = true
        doubleTap(checkbox)
        assertEquals("tap on a consuming child never reaches the RecyclerView listener", 0, doubleTaps)

        // The name text does not consume touches, so that tap is seen.
        doubleTap(rowView(1, R.id.tvName))
        assertEquals(1, doubleTaps)
    }

    // ---- Shop mode: summary list ----

    @Test
    fun shop_doubleTapOnAnyPartOfARowReportsThatCategory() {
        val tapped = mutableListOf<ShoppingCategory>()
        val adapter = CategorySummaryAdapter { tapped.add(it) }
        recyclerView.adapter = adapter
        adapter.submitList(categories)
        layOut()

        categories.forEachIndexed { i, cat ->
            doubleTap(rowView(i, R.id.tvName))
            doubleTap(rowView(i, R.id.tvCount))
            assertEquals(listOf(cat, cat), tapped.takeLast(2))
        }
        assertEquals(6, tapped.size)
    }

    @Test
    fun shop_reportsTheCategoryNowInTheRowAfterAReorder() {
        val tapped = mutableListOf<ShoppingCategory>()
        val adapter = CategorySummaryAdapter { tapped.add(it) }
        recyclerView.adapter = adapter
        adapter.submitList(categories)
        layOut()
        val reordered = listOf(categories[1], categories[2], categories[0])
        adapter.submitList(reordered)
        waitUntil { adapter.currentList.first().id == 3L }
        layOut()

        doubleTap(rowView(0, R.id.tvName))
        assertEquals(listOf(categories[1]), tapped)
    }

    // ---- helpers ----

    private fun planAdapterWithoutRowListener() {
        ItemAdapter(onAction = { _, _ -> }, mode = Mode.PLAN).also {
            recyclerView.adapter = it
            it.submitList(planRows(categories))
            layOut()
        }
    }

    private fun layOut() {
        shadowOf(Looper.getMainLooper()).idle()
        val w = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val h = View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY)
        recyclerView.measure(w, h)
        recyclerView.layout(0, 0, 1080, 2400)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (!condition() && SystemClock.uptimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("list update never arrived", condition())
    }

    /** The view with [id] inside the row at adapter [position]. */
    private fun rowView(position: Int, id: Int): View =
        recyclerView.getChildAt(position).findViewById(id)

    private fun doubleTap(target: View) {
        val t = SystemClock.uptimeMillis()
        tap(target, downTime = t)
        tap(target, downTime = t + 120)
    }

    private fun doubleTapAt(row: View, x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        tapAt(row, x, y, t)
        tapAt(row, x, y, t + 120)
    }

    /** Taps the centre of [target], dispatching from the RecyclerView down. */
    private fun tap(target: View, downTime: Long) {
        val r = Rect(0, 0, target.width, target.height)
        recyclerView.offsetDescendantRectToMyCoords(target, r)
        dispatch(r.exactCenterX(), r.exactCenterY(), downTime)
    }

    private fun tapAt(row: View, x: Float, y: Float, downTime: Long) {
        dispatch(row.left + x, row.top + y, downTime)
    }

    private fun dispatch(x: Float, y: Float, downTime: Long) {
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(downTime, downTime + 30, MotionEvent.ACTION_UP, x, y, 0)
        recyclerView.dispatchTouchEvent(down)
        recyclerView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }
}
