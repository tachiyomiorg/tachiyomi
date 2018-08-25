@file:Suppress("PackageDirectoryMismatch")

package android.support.v7.widget

import android.support.v7.widget.RecyclerView.NO_POSITION
import eu.kanade.tachiyomi.ui.reader.ReaderActivity

/**
 * Layout manager used by the webtoon viewer. Item prefetch is disabled because the extra layout
 * space feature is used which allows setting the image even if the holder is not visible,
 * avoiding (in most cases) black views when they are visible.
 *
 * This layout manager uses the same package name as the support library in order to use a package
 * protected method.
 */
class WebtoonLayoutManager(activity: ReaderActivity) : LinearLayoutManager(activity) {

    /**
     * Extra layout space is set to half the screen height.
     */
    private val extraLayoutSpace = activity.resources.displayMetrics.heightPixels / 2

    init {
        isItemPrefetchEnabled = false
    }

    /**
     * Returns the custom extra layout space.
     */
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        return extraLayoutSpace
    }

    /**
     * Returns the position of the last item whose end side is visible on screen.
     */
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
