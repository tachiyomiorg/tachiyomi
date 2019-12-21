package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Seekbar to show current chapter progress.
 */
class ReaderSeekBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : AppCompatSeekBar(context, attrs) {

    /**
     * Whether the seekbar should draw from right to left.
     */
    var isRTL = false

    /**
     * Draws the seekbar, translating the canvas if using a right to left reader.
     */
    override fun draw(canvas: Canvas) {
        if (isRTL) {
            val px = width / 2f
            val py = height / 2f

            canvas.scale(-1f, 1f, px, py)
        }
        super.draw(canvas)
    }

    /**
     * Handles touch events, translating coordinates if using a right to left reader.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isRTL) {
            event.setLocation(width - event.x, event.y)
        }
        return super.onTouchEvent(event)
    }

}
