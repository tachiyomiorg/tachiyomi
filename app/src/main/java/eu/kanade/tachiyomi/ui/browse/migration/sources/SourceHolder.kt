package eu.kanade.tachiyomi.ui.browse.migration.sources

import android.view.View
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.source_main_controller_card_item.image
import kotlinx.android.synthetic.main.source_main_controller_card_item.title

class SourceHolder(view: View, val adapter: SourceAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    fun bind(item: SourceItem) {
        val source = item.source

        // Set source name
        title.text = source.name

        // Set source icon
        itemView.post {
            image.setImageDrawable(source.icon())
        }
    }
}
