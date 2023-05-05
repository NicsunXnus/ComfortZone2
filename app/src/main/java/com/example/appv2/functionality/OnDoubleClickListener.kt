package com.example.appv2.functionality

import android.os.Handler
import android.os.Looper
import android.view.View

class OnDoubleClickListener(
    private val singleClickAction: () -> Unit,
    private val doubleClickAction: () -> Unit
) : View.OnClickListener {

    private val DOUBLE_CLICK_TIME_DELTA: Long = 300 // milliseconds
    private var lastClickTime: Long = 0

    override fun onClick(v: View) {
        val clickTime = System.currentTimeMillis()
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            doubleClickAction()
            lastClickTime = 0
        } else {
            lastClickTime = clickTime
            Handler(Looper.getMainLooper()).postDelayed({
                if (lastClickTime != 0L) {
                    singleClickAction()
                }
                lastClickTime = 0
            }, DOUBLE_CLICK_TIME_DELTA)
        }
    }
}

