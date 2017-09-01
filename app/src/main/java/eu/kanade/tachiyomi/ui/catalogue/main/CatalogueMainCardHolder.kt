package eu.kanade.tachiyomi.ui.catalogue.main

import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.util.getRound
import eu.kanade.tachiyomi.util.gone
import kotlinx.android.synthetic.main.catalogue_main_controller_card_item.view.*

/**
 * Holder that binds the [CatalogueMainCardItem] containing source item.
 *
 * @param view view of [CatalogueMainCardItem]
 * @param adapter instance of [CatalogueMainCardAdapter]
 */
class CatalogueMainCardHolder(view: View, adapter: CatalogueMainCardAdapter) : FlexibleViewHolder(view, adapter) {

    init {
        // Call onBrowseClickListener when item is pressed.
        itemView.setOnClickListener {
            val item = adapter.getItem(adapterPosition)
            adapter.browseClickListener.onBrowseClickListener(item)
        }

        // Call onBrowseClickListener when browse is pressed.
        itemView.source_browse.setOnClickListener {
            val item = adapter.getItem(adapterPosition)
            adapter.browseClickListener.onBrowseClickListener(item)
        }

        // Call OnLatestClickListener when latest is pressed.
        itemView.source_latest.setOnClickListener {
            val item = adapter.getItem(adapterPosition)
            adapter.latestClickListener.OnLatestClickListener(item)
        }
    }

    /**
     * Bind the [CatalogueMainCardItem] to the view.
     *
     * @param source item containing source information.
     */
    fun bind(source: CatalogueSource) {
        with(itemView) {
            // Set source name
            title.text = source.name

            // Set circle letter image.
            post {
                image.setImageDrawable(image.getRound(source.name.take(1).toUpperCase(),false))
            }

            // If source is login, show only login option
            if (source is LoginSource && !source.isLogged()) {
                source_browse.text = context.getString(R.string.login)
                source_latest.gone()
            }
        }
    }
}