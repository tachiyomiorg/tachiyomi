package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import eu.kanade.tachiyomi.R

class ThemedSwipeRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SwipeRefreshLayout(context, attrs) {

    init {
        setColors()
    }

    private fun setColors() {
        // Background is controlled with "swipeRefreshLayoutProgressSpinnerBackgroundColor" in XML

        // This updates the progress arrow color
        val white = ContextCompat.getColor(context, R.color.md_white_1000)
        setColorSchemeColors(white, white, white)
    }
}
