package eu.kanade.tachiyomi.widget

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PreCachingLayoutManager(context: Context) : LinearLayoutManager(context) {

    init {
        isItemPrefetchEnabled = false
    }

    companion object {
        const val DEFAULT_EXTRA_LAYOUT_SPACE = 600
    }

    var extraLayoutSpace = 0

    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        if (extraLayoutSpace > 0) {
            return extraLayoutSpace
        }
        return DEFAULT_EXTRA_LAYOUT_SPACE
    }

}
