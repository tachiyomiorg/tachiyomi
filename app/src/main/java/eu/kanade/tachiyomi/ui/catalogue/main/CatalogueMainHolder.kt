package eu.kanade.tachiyomi.ui.catalogue.main

import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.catalogue.CatalogueController
import eu.kanade.tachiyomi.ui.catalogue.main.card.CatalogueMainCardAdapter
import eu.kanade.tachiyomi.ui.catalogue.main.card.CatalogueMainCardItem
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import kotlinx.android.synthetic.main.catalogue_main_controller_card.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.*

class CatalogueMainHolder(view: View, val adapter: CatalogueMainAdapter) : FlexibleViewHolder(view, adapter), FlexibleAdapter.OnItemClickListener {
    /**
     * Adapter containing sources
     */
    private var adapterSource = CatalogueMainCardAdapter(this)

    val prefs: PreferencesHelper = Injekt.get()
    val controller = adapter.controller

    override fun onItemClick(position: Int): Boolean {
        val item = adapterSource.getItem(position)
        val source = item.source
        if (source is LoginSource && !source.isLogged()) {
            val dialog = SourceLoginDialog(source)
            dialog.targetController = controller
            dialog.showDialog(controller.router)
        } else {
            // Update last used
            setLastUsedSource(item.source.id)
            if (controller.firstUse)
                controller.presenter.loadRecentSources()
            // Open the catalogue view.
            controller.router.pushController(RouterTransaction.with(CatalogueController(null, item.source))
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler()))
        }
        return false
    }

    fun setLastUsedSource(key: Long) {
        prefs.lastUsedCatalogueSource().set(key)
    }

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