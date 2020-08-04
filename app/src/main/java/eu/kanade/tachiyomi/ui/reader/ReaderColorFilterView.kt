package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toXfermode

class ReaderColorFilterView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val colorFilterPaint: Paint = Paint()

    fun setFilterColor(color: Int, filterMode: Int) {
        colorFilterPaint.color = color
        colorFilterPaint.xfermode = when (filterMode) {
            1 -> PorterDuff.Mode.MULTIPLY
            2 -> PorterDuff.Mode.SCREEN
            3 -> PorterDuff.Mode.OVERLAY
            4 -> PorterDuff.Mode.LIGHTEN
            5 -> PorterDuff.Mode.DARKEN
            else -> PorterDuff.Mode.SRC_OVER
        }.toXfermode()

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(colorFilterPaint)
    }
}
