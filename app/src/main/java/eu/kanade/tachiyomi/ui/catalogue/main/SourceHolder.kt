package eu.kanade.tachiyomi.ui.catalogue.main

import android.os.Build
import android.view.View
import android.view.ViewGroup
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.util.dpToPx
import eu.kanade.tachiyomi.util.getRound
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.visible
import io.github.mthli.slice.Slice
import kotlinx.android.synthetic.main.catalogue_main_controller_card_item.view.*

class SourceHolder(view: View, adapter: CatalogueMainAdapter) : FlexibleViewHolder(view, adapter) {

    private val slice = Slice(itemView.card).apply {
        setColor(adapter.cardBackground)
    }

    init {
        itemView.source_browse.setOnClickListener {
            adapter.browseClickListener.onBrowseClick(adapterPosition)
        }

        itemView.source_latest.setOnClickListener {
            adapter.latestClickListener.onLatestClick(adapterPosition)
        }
    }

    fun bind(item: SourceItem) {
        val source = item.source
        with(itemView) {
            setCardEdges(item)

            // Set source name
            title.text = source.name

            // Set circle letter image.
            post {
                image.setImageDrawable(image.getRound(source.name.take(1).toUpperCase(),false))
            }

            // If source is login, show only login option
            if (source is LoginSource && !source.isLogged()) {
                source_browse.setText(R.string.login)
                source_latest.gone()
            } else {
                source_browse.setText(R.string.browse)
                source_latest.visible()
            }
        }
    }

    private fun setCardEdges(item: SourceItem) {
        // Position of this item in its header. Defaults to 0 when header is null.
        var position = 0

        // Number of items in the header of this item. Defaults to 1 when header is null.
        var count = 1

        if (item.header != null) {
            val sectionItems = mAdapter.getSectionItems(item.header)
            position = sectionItems.indexOf(item)
            count = sectionItems.size
        }

        when {
            // Only one item in the card
            count == 1 -> applySlice(2f, false, false, true, true)
            // First item of the card
            position == 0 -> applySlice(2f, false, true, true, false)
            // Last item of the card
            position == count - 1 -> applySlice(2f, true, false, false, true)
            // Middle item
            else -> applySlice(0f, false, false, false, false)
        }
    }

    private fun applySlice(radius: Float, topRect: Boolean, bottomRect: Boolean,
                           topShadow: Boolean, bottomShadow: Boolean) {

        slice.setRadius(radius)
        slice.showLeftTopRect(topRect)
        slice.showRightTopRect(topRect)
        slice.showLeftBottomRect(bottomRect)
        slice.showRightBottomRect(bottomRect)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            slice.showTopEdgeShadow(topShadow)
            slice.showBottomEdgeShadow(bottomShadow)
        }
        setMargins(margin, if (topShadow) margin else 0, margin, if (bottomShadow) margin else 0)
    }

    private fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val v = itemView.card
        if (v.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = v.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
        }
    }

    companion object {
        val margin = 8.dpToPx
    }
}