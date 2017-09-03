package eu.kanade.tachiyomi.ui.catalogue.main

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
        val slice = Slice(itemView.frame)
        slice.setElevation(2f)

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
            count == 1 -> {
                slice.setRadius(2f)
                slice.showLeftTopRect(false)
                slice.showRightTopRect(false)
                slice.showRightBottomRect(true)
                slice.showLeftBottomRect(true)
                slice.showTopEdgeShadow(true)
                slice.showBottomEdgeShadow(true)
                setMargins(margins, margins, margins, margins)
            }
            // First item of the card
            position == 0 -> {
                slice.setRadius(2f)
                slice.showLeftTopRect(false)
                slice.showRightTopRect(false)
                slice.showRightBottomRect(true)
                slice.showLeftBottomRect(true)
                slice.showTopEdgeShadow(true)
                slice.showBottomEdgeShadow(false)
                setMargins(margins, margins, margins, 0)
            }
            // Last item of the card
            position == count - 1 -> {
                slice.setRadius(2f)
                slice.showLeftTopRect(true)
                slice.showRightTopRect(true)
                slice.showRightBottomRect(false)
                slice.showLeftBottomRect(false)
                slice.showTopEdgeShadow(false)
                slice.showBottomEdgeShadow(true)
                setMargins(margins, 0, margins, margins)
            }
            // Middle item
            else -> {
                slice.setRadius(0.0f)
                slice.showTopEdgeShadow(false)
                slice.showBottomEdgeShadow(false)
                setMargins(margins, 0, margins, 0)
            }
        }
    }

    private fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val v = itemView.frame
        if (v.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = v.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            v.requestLayout()
        }
    }

    companion object {
        val margins = 8.dpToPx
    }
}