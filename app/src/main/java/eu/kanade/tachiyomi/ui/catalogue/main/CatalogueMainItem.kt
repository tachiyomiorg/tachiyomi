package eu.kanade.tachiyomi.ui.catalogue.main

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.inflate

class CatalogueMainItem(val sourcePair: Pair<String, List<CatalogueSource>>) : AbstractFlexibleItem<CatalogueMainHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueMainHolder {
        return CatalogueMainHolder(parent.inflate(layoutRes), adapter as CatalogueMainAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueMainHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(sourcePair)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CatalogueMainItem) {
            return sourcePair.first == other.sourcePair.first
        }
        return false
    }

    override fun hashCode(): Int {
        return sourcePair.first.hashCode()
    }

}