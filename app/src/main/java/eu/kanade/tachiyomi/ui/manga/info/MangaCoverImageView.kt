package eu.kanade.tachiyomi.ui.manga.info

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

/**
 * A custom ImageView for holding a manga cover with:
 * - width: min(maxWidth attr, 33% of parent width)
 * - height: 2:3 width:height ratio
 *
 * Should be defined with a width of match_parent.
 */
class MangaCoverImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = min(maxWidth, measuredWidth / 3)
        val height = width / 2 * 3
        setMeasuredDimension(width, height)
    }
}
