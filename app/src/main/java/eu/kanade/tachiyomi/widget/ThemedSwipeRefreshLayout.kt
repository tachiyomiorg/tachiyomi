package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.aboutlibraries.util.getThemeColor
import eu.kanade.tachiyomi.R

class ThemedSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {
    init {
        // Background
        setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.colorPrimary))
        // This updates the progress arrow color
        setColorSchemeColors(context.getThemeColor(R.attr.colorOnPrimary))
    }
}
