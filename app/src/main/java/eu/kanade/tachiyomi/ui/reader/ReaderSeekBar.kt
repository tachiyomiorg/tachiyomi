package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar
import com.mikepenz.aboutlibraries.util.getThemeColor
import eu.kanade.tachiyomi.R

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

    init {
        // Set color to onPrimary when ColoredBars theme is applied
        if (context.getThemeColor(R.attr.colorToolbar) == context.getThemeColor(R.attr.colorPrimary)) {
            thumbTintList = ColorStateList.valueOf(context.getThemeColor(R.attr.colorOnPrimary))
            progressTintList = thumbTintList
        }
    }
}
