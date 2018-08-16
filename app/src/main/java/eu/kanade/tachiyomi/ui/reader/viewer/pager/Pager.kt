package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.content.Context
import android.support.v4.view.DirectionalViewPager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import eu.kanade.tachiyomi.ui.reader.viewer.LongTapGestureDetector

open class Pager(
        context: Context,
        isHorizontal: Boolean = true
) : DirectionalViewPager(context, isHorizontal) {

    var tapListener: ((MotionEvent) -> Unit)? = null
    var longTapListener: ((MotionEvent) -> Unit)? = null

    private var isTapListenerEnabled = true

    private val gestureListener = object : LongTapGestureDetector.Listener() {

        override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
            tapListener?.invoke(ev)
            return true
        }

        override fun onLongTapConfirmed(ev: MotionEvent) {
            val listener = longTapListener
            if (listener != null) {
                listener.invoke(ev)
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }

    }

    private val gestureDetector = LongTapGestureDetector(context, gestureListener)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(ev)
        if (isTapListenerEnabled) {
            gestureDetector.onTouchEvent(ev)
        }
        return handled
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.onTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun executeKeyEvent(event: KeyEvent): Boolean {
        // Disable viewpager's default key event handling
        return false
    }

    fun setTapListenerEnabled(enabled: Boolean) {
        isTapListenerEnabled = enabled
    }

}
