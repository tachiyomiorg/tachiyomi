@file:Suppress("PackageDirectoryMismatch")

package android.support.v7.widget

import android.content.Context
import android.support.v7.widget.RecyclerView.NO_POSITION

class WebtoonLayoutManager(context: Context) : LinearLayoutManager(context) {

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

    fun findLastEndVisibleItemPosition(): Int {
        ensureLayoutState()
        @ViewBoundsCheck.ViewBounds val preferredBoundsFlag =
                (ViewBoundsCheck.FLAG_CVE_LT_PVE or ViewBoundsCheck.FLAG_CVE_EQ_PVE)

        val fromIndex = childCount - 1
        val toIndex = -1

        val child = if (mOrientation == HORIZONTAL)
            mHorizontalBoundCheck
                .findOneViewWithinBoundFlags(fromIndex, toIndex, preferredBoundsFlag, 0)
        else
            mVerticalBoundCheck
                .findOneViewWithinBoundFlags(fromIndex, toIndex, preferredBoundsFlag, 0)

        return if (child == null) NO_POSITION else getPosition(child)
    }

}
