package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import eu.davidea.fastscroller.FastScroller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx

class MaterialFastScroll @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FastScroller(context, attrs) {

    init {
        setViewsToUse(
            R.layout.material_fastscroll, R.id.fast_scroller_bubble, R.id.fast_scroller_handle
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isHidden) return false
        return super.onTouchEvent(event)
    }

    override fun setBubbleAndHandlePosition(y: Float) {
        super.setBubbleAndHandlePosition(y)
        bubble.y = handle.y - bubble.height / 2f + handle.height / 2f
        bubble.translationX = (-45).dpToPx.toFloat()
    }
}
