package eu.kanade.tachiyomi.ui.catalogue.main.card

import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.util.getRound
import eu.kanade.tachiyomi.util.gone
import kotlinx.android.synthetic.main.catalogue_main_controller_card_item.view.*

class CatalogueMainCardHolder(view: View, val adapter: CatalogueMainCardAdapter) : FlexibleViewHolder(view, adapter) {
    fun bind(source: CatalogueSource) {
        with(itemView) {
            title.text = source.name
            // Update circle letter image.
            post {
                image.setImageDrawable(image.getRound(source.name.take(1).toUpperCase(),false))
            }

            if (source is LoginSource && !source.isLogged()) {
                source_option.text = context.getString(eu.kanade.tachiyomi.R.string.login)
                source_browse.gone()
            }
        }
    }
}