package com.example.appv2.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.widget.NestedScrollView

class CustomNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var mTouchSlop: Int
    private var mMinimumVelocity: Int
    private var mMaximumVelocity: Int

    init {
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity

        // Set custom values for the touch slop and fling velocities
        val customTouchSlop = 8 // You can adjust this value to your preference
        val customMinFlingVelocity = 100 // You can adjust this value to your preference
        val customMaxFlingVelocity = 10000 // You can adjust this value to your preference

        mTouchSlop = customTouchSlop
        mMinimumVelocity = customMinFlingVelocity
        mMaximumVelocity = customMaxFlingVelocity
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return super.onTouchEvent(swapEvent(ev))
    }

    private fun swapEvent(ev: MotionEvent): MotionEvent {
        val newX = ev.y
        val newY = ev.x
        ev.setLocation(newX, newY)
        return ev
    }
}
