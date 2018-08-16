package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import android.view.MotionEvent

class ReaderSeekBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : AppCompatSeekBar(context, attrs) {

    var isReversed = false

    override fun draw(canvas: Canvas) {
        if (isReversed) {
            val px = width / 2f
            val py = height / 2f

            canvas.scale(-1f, 1f, px, py)
        }
        super.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isReversed) {
            event.setLocation(width - event.x, event.y)
        }
        return super.onTouchEvent(event)
    }

}
