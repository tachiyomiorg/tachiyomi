package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v7.widget.AppCompatTextView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ScaleXSpan
import android.util.AttributeSet
import android.widget.TextView

/**
 * Page indicator found at the bottom of the reader
 */
class PageIndicatorTextView(
        context: Context,
        attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private val fillColor = Color.rgb(235, 235, 235)
    private val strokeColor = Color.rgb(45, 45, 45)

    override fun onDraw(canvas: Canvas) {
        textColorField.set(this, strokeColor)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        super.onDraw(canvas)

        textColorField.set(this, fillColor)
        paint.strokeWidth = 0f
        paint.style = Paint.Style.FILL
        super.onDraw(canvas)
    }

    @SuppressLint("SetTextI18n")
    override fun setText(text: CharSequence?, type: BufferType?) {
        // Add spaces at the start & end of the text, otherwise the stroke is cut-off because it's
        // not taken into account when measuring the text (view's padding doesn't help).
        val currText = " $text "

        // Also add a bit of spacing between each character, as the stroke overlaps them
        val finalText = SpannableString(currText.asIterable().joinToString("\u00A0"))

        for (i in 1..finalText.lastIndex step 2) {
            finalText.setSpan(ScaleXSpan(0.1f), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        super.setText(finalText, TextView.BufferType.SPANNABLE)
    }

    private companion object {
        // We need to use reflection to set the text color instead of using [setTextColor],
        // otherwise the view is invalidated inside [onDraw] and there's an infinite loop
        val textColorField = TextView::class.java.getDeclaredField("mCurTextColor").apply {
            isAccessible = true
        }!!
    }
}
