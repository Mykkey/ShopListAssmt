package com.mmgpt.shoplistgpt.adapter

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

/**
 * Calls [onDoubleTap] when this row is double tapped.
 *
 * The detector is attached to the row itself so the tapped row is known
 * directly, rather than being re-derived from touch coordinates on the parent
 * RecyclerView (which misses taps that a child view consumes or that land in
 * the gap between rows).
 */
@SuppressLint("ClickableViewAccessibility")
fun View.setOnRowDoubleTapListener(onDoubleTap: () -> Unit) {
    val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap()
            return true
        }
    })
    setOnTouchListener { v, event ->
        detector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) v.performClick()
        true
    }
}
