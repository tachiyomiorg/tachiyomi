package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

class ThemedSwipeRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        SwipeRefreshLayout(context, attrs) {

    init {
        setColors()
    }

    private fun setColors() {
        setProgressBackgroundColorSchemeColor(context.getResourceColor(R.attr.colorAccent))
        setColorSchemeColors(
                ContextCompat.getColor(context, R.color.md_white_1000),
                ContextCompat.getColor(context, R.color.md_white_1000),
                ContextCompat.getColor(context, R.color.md_white_1000))
    }
}
