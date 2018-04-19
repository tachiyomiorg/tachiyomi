package eu.kanade.tachiyomi.ui.migration

import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.holder.SlicedHolder
import eu.kanade.tachiyomi.util.getRound
import eu.kanade.tachiyomi.util.gone
import io.github.mthli.slice.Slice
import kotlinx.android.synthetic.main.catalogue_main_controller_card_item.*

class SourceHolder(view: View, override val adapter: SourceAdapter) :
        BaseFlexibleViewHolder(view, adapter),
        SlicedHolder {

    override val slice = Slice(card).apply {
        setColor(adapter.cardBackground)
    }

    override val viewToSlice: View
        get() = card

    init {
        source_latest.gone()
        source_browse.setText(R.string.select)
        source_browse.setOnClickListener {
            adapter.selectClickListener?.onSelectClick(adapterPosition)
        }
    }

    fun bind(item: SourceItem) {
        val source = item.source
        setCardEdges(item)

        // Set source name
        title.text = source.name

        // Set circle letter image.
        itemView.post {
            image.setImageDrawable(image.getRound(source.name.take(1).toUpperCase(),false))
        }
    }
}