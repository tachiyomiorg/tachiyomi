package eu.kanade.tachiyomi.ui.catalogue_new.search_result

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.inflate

class CatalogueSearchItem(val searchResult: Pair<List<Manga>, Source>) : AbstractFlexibleItem<CatalogueSearchHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_search_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueSearchHolder {
        return CatalogueSearchHolder(parent.inflate(layoutRes), adapter as CatalogueSearchAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueSearchHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(searchResult)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CatalogueSearchItem) {
            return searchResult.second.id == other.searchResult.second.id
        }
        return false
    }

    override fun hashCode(): Int {
        return searchResult.second.id.toInt()
    }

}