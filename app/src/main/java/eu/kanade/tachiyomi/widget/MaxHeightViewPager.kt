package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.forEach
import androidx.viewpager.widget.ViewPager

/**
 * A [ViewPager] that sets its height to the maximum height of its children.
 * This is a way to mimic WRAP_CONTENT for its height.
 */
class MaxHeightViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measuredHeight = heightMeasureSpec

        var height = 0
        forEach {
            it.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val h = it.measuredHeight
            if (h > height) height = h
        }
        if (height != 0) {
            measuredHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, measuredHeight)
    }
}
