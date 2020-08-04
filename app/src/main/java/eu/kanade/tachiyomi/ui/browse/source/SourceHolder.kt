package eu.kanade.tachiyomi.ui.browse.source

import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.holder.SlicedHolder
import eu.kanade.tachiyomi.ui.browse.SourceListItem
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.setVectorCompat
import io.github.mthli.slice.Slice
import kotlinx.android.synthetic.main.source_main_controller_card_item.card
import kotlinx.android.synthetic.main.source_main_controller_card_item.image
import kotlinx.android.synthetic.main.source_main_controller_card_item.pin
import kotlinx.android.synthetic.main.source_main_controller_card_item.source_latest
import kotlinx.android.synthetic.main.source_main_controller_card_item.title

class SourceHolder(private val view: View, override val adapter: SourceAdapter) :
    BaseFlexibleViewHolder(view, adapter),
    SourceListItem,
    SlicedHolder {

    override val slice = Slice(card).apply {
        setColor(adapter.cardBackground)
    }

    override val viewToSlice: View
        get() = card

    init {
        source_latest.setOnClickListener {
            adapter.clickListener.onLatestClick(bindingAdapterPosition)
        }

        pin.setOnClickListener {
            adapter.clickListener.onPinClick(bindingAdapterPosition)
        }
    }

    fun bind(item: SourceItem) {
        val source = item.source
        setCardEdges(item)

        // Set source name
        title.text = source.name

        // Set source icon
        itemView.post {
            val icon = source.icon()
            when {
                icon != null -> image.setImageDrawable(icon)
                item.source.id == LocalSource.ID -> image.setImageResource(R.mipmap.ic_local_source)
            }
        }

        source_latest.isVisible = source.supportsLatest

        pin.isVisible = true
        if (item.isPinned) {
            pin.setVectorCompat(R.drawable.ic_push_pin_filled_24dp, view.context.getResourceColor(R.attr.colorAccent))
        } else {
            pin.setVectorCompat(R.drawable.ic_push_pin_24dp, view.context.getResourceColor(android.R.attr.textColorHint))
        }
    }
}
