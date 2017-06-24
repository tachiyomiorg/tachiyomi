package eu.kanade.tachiyomi.ui.catalogue.main

import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.catalogue.CatalogueController
import eu.kanade.tachiyomi.ui.catalogue.main.card.CatalogueMainCardAdapter
import eu.kanade.tachiyomi.ui.catalogue.main.card.CatalogueMainCardItem
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import kotlinx.android.synthetic.main.catalogue_main_controller_card.view.*
import java.util.*

/**
 * Binds the [CatalogueMainItem] to the view.
 */
class CatalogueMainHolder(view: View, val adapter: CatalogueMainAdapter) : FlexibleViewHolder(view, adapter) {
    /**
     * Adapter containing sources
     */
    private var adapterSource = CatalogueMainCardAdapter(adapter.controller)

    fun bind(sourcePair: Pair<String, List<CatalogueSource>>) {
        with(itemView) {
            val lang = sourcePair.first

            when (lang){
                "" -> title.text = context.getString(R.string.local)
                "recent" -> title.gone()
                else -> {
                    val locale = Locale(sourcePair.first)
                    title.text = locale.getDisplayName(locale).capitalize()
                }
            }

            // Add the sources to the adapter
            recycler.layoutManager = LinearLayoutManager(context)
            val dividerItemDecoration = DividerItemDecoration( recycler.context,
                    ( recycler.layoutManager as LinearLayoutManager).orientation)
            recycler.addItemDecoration(dividerItemDecoration)
            recycler.setHasFixedSize(true)
            recycler.adapter = adapterSource
            adapterSource.updateDataSet(sourcePair.second.map(::CatalogueMainCardItem))
        }
    }
}