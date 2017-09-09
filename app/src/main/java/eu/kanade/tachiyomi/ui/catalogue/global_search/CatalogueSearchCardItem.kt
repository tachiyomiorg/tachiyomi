package eu.kanade.tachiyomi.ui.catalogue.global_search

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate

class CatalogueSearchCardItem(val manga: Manga) : AbstractFlexibleItem<CatalogueSearchCardHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_global_search_controller_card_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueSearchCardHolder {
        return CatalogueSearchCardHolder(parent.inflate(layoutRes), adapter as CatalogueSearchCardAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: CatalogueSearchCardHolder,
                                position: Int, payloads: List<Any?>?) {
        holder.bind(manga)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CatalogueSearchCardItem) {
            return manga.id == other.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id?.toInt() ?: 0
    }

}