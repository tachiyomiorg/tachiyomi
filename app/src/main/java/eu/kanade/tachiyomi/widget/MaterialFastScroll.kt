package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import eu.davidea.fastscroller.FastScroller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPxEnd

class MaterialFastScroll @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FastScroller(context, attrs) {

    init {
        setViewsToUse(
            R.layout.material_fastscroll,
            R.id.fast_scroller_bubble,
            R.id.fast_scroller_handle
        )
        autoHideEnabled = true
        ignoreTouchesOutsideHandle = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isHidden) return false
        return super.onTouchEvent(event)
    }

    override fun setBubbleAndHandlePosition(y: Float) {
        super.setBubbleAndHandlePosition(y)
        if (bubbleEnabled) {
            bubble.y = handle.y - bubble.height / 2f + handle.height / 2f
            bubble.translationX = (-45f).dpToPxEnd
        }
    }
}
