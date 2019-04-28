package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import uy.kohesive.injekt.injectLazy


class ReaderColorFilterView(
        context: Context,
        attrs: AttributeSet? = null
) : View(context, attrs) {

    private val preferences by injectLazy<PreferencesHelper>()

    private val colorFilterPaint: Paint = Paint()

    override fun setBackgroundColor(color: Int) {
        colorFilterPaint.setColor(color)
        colorFilterPaint.xfermode = PorterDuffXfermode(when (preferences.colorFilterMode().getOrDefault()) {
            0 -> PorterDuff.Mode.SRC_OVER
            1 -> PorterDuff.Mode.OVERLAY
            2 -> PorterDuff.Mode.MULTIPLY
            3 -> PorterDuff.Mode.SCREEN
            4 -> PorterDuff.Mode.LIGHTEN
            else -> PorterDuff.Mode.DARKEN
        })
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(colorFilterPaint)
    }
}