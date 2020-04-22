package eu.kanade.tachiyomi.ui.source

import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.holder.SlicedHolder
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.roundTextIcon
import eu.kanade.tachiyomi.util.view.visible
import io.github.mthli.slice.Slice
import kotlinx.android.synthetic.main.source_main_controller_card_item.card
import kotlinx.android.synthetic.main.source_main_controller_card_item.image
import kotlinx.android.synthetic.main.source_main_controller_card_item.source_browse
import kotlinx.android.synthetic.main.source_main_controller_card_item.source_latest
import kotlinx.android.synthetic.main.source_main_controller_card_item.title

class SourceHolder(view: View, override val adapter: SourceAdapter) :
        BaseFlexibleViewHolder(view, adapter),
        SlicedHolder {

    override val slice = Slice(card).apply {
        setColor(adapter.cardBackground)
    }

    override val viewToSlice: View
        get() = card

    init {
        source_browse.setOnClickListener {
            adapter.browseClickListener.onBrowseClick(bindingAdapterPosition)
        }

        source_latest.setOnClickListener {
            adapter.latestClickListener.onLatestClick(bindingAdapterPosition)
        }
    }

    fun bind(item: SourceItem) {
        val source = item.source
        setCardEdges(item)

        // Set source name
        title.text = source.name

        // Set circle letter image.
        itemView.post {
            val icon = source.icon()
            if (icon != null) image.setImageDrawable(icon)
            else image.roundTextIcon(source.name)
        }

        source_browse.setText(R.string.browse)
        if (source.supportsLatest) {
            source_latest.visible()
        } else {
            source_latest.gone()
        }
    }
}
