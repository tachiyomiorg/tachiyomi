package eu.kanade.tachiyomi.ui.catalogue.main

import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.catalogue.main.card.CatalogueMainCardAdapter
import eu.kanade.tachiyomi.ui.catalogue.main.card.CatalogueMainCardItem
import eu.kanade.tachiyomi.util.gone
import kotlinx.android.synthetic.main.catalogue_main_controller_card.view.*
import java.util.*

/**
 * Holder that binds the [CatalogueMainItem] containing catalogue cards.
 *
 * @param view view of [CatalogueMainItem]
 * @param adapter instance of [CatalogueMainAdapter]
 */
class CatalogueMainHolder(view: View, adapter: CatalogueMainAdapter) : FlexibleViewHolder(view, adapter) {
    /**
     * Adapter containing sources
     */
    private var adapterSource = CatalogueMainCardAdapter(adapter.controller)

    /**
     * Bind the [CatalogueMainItem] to the view.
     *
     * @param sourcePair contains catalogue card language and source information.
     */
    fun bind(sourcePair: Pair<String, List<CatalogueSource>>) {
        with(itemView) {
            // Get language
            val lang = sourcePair.first

            // Update card header by language.
            when (lang){
                "" -> title.text = context.getString(R.string.other_source)
                "local" -> title.text = context.getString(R.string.local_source)
                "recent" -> title.gone()
                else -> {
                    val locale = Locale(sourcePair.first)
                    title.text = locale.getDisplayName(locale).capitalize()
                }
            }

            // Add the sources to the adapter
            recycler.layoutManager = LinearLayoutManager(context)

            // Add divider line.
            val dividerItemDecoration = DividerItemDecoration( recycler.context,
                    ( recycler.layoutManager as LinearLayoutManager).orientation)
            recycler.addItemDecoration(dividerItemDecoration)

            // Set adapter.
            recycler.adapter = adapterSource
            adapterSource.updateDataSet(sourcePair.second.map(::CatalogueMainCardItem))
        }
    }
}