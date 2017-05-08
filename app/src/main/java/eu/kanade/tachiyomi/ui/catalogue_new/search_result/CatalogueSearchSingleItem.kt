package eu.kanade.tachiyomi.ui.catalogue_new.search_result

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate

class CatalogueSearchSingleItem(val manga: Manga) : AbstractFlexibleItem<CatalogueSearchSingleHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_search_single
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueSearchSingleHolder {
        return CatalogueSearchSingleHolder(parent.inflate(layoutRes), adapter as CatalogueSearchSingleAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueSearchSingleHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(manga)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CatalogueSearchSingleItem) {
            return manga.id == other.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id?.toInt() ?: 0
    }

}