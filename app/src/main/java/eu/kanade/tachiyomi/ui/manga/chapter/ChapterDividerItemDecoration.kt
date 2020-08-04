package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.forEach
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView

/**
 * Mimics a DividerItemDecoration that doesn't draw between the first two items.
 *
 * Used in MangaController since the manga info header and chapters header are the first two
 * items in the list using a ConcatAdapter.
 */
class ChapterDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val divider: Drawable

    init {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
        divider = a.getDrawable(0)!!
        a.recycle()
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null) {
            return
        }

        canvas.save()
        parent.forEach {
            val top = it.bottom + it.marginBottom
            val bottom = top + divider.intrinsicHeight
            val left = parent.paddingStart
            val right = parent.width - parent.paddingEnd
            divider.setBounds(left, top, right, bottom)
            divider.draw(canvas)
        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)

        if (position == 0) {
            outRect.setEmpty()
        } else {
            outRect.set(0, 0, 0, divider.intrinsicHeight)
        }
    }
}
