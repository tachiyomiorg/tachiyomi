package eu.kanade.tachiyomi.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.annotation.Dimension

/**
 * Source: https://github.com/santaevpavel
 *
 * A class that draws the outlines of a text when given a stroke color and stroke width.
 */
class OutlineSpan(
    @ColorInt private val strokeColor: Int,
    @Dimension private val strokeWidth: Float
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return paint.measureText(text.toString().substring(start until end)).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val originTextColor = paint.color

        paint.apply {
            color = strokeColor
            style = Paint.Style.STROKE
            this.strokeWidth = this@OutlineSpan.strokeWidth
        }
        canvas.drawText(text, start, end, x, y.toFloat(), paint)

        paint.apply {
            color = originTextColor
            style = Paint.Style.FILL
        }

        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }
}
