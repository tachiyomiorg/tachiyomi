package eu.kanade.tachiyomi.ui.catalogue

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.catalogue_grid_item.view.*

class CatalogueItem(val manga: Manga) : AbstractFlexibleItem<CatalogueHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_grid_item
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>,
                                  inflater: LayoutInflater,
                                  parent: ViewGroup): CatalogueHolder {

        if (parent is AutofitRecyclerView) {
            val view = parent.inflate(R.layout.catalogue_grid_item).apply {
                card.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, parent.itemWidth / 3 * 4)
                gradient.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT, parent.itemWidth / 3 * 4 / 2, Gravity.BOTTOM)
            }
            return CatalogueGridHolder(view, adapter)
        } else {
            val view = parent.inflate(R.layout.catalogue_list_item)
            return CatalogueListHolder(view, adapter)
        }
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>,
                                holder: CatalogueHolder,
                                position: Int,
                                payloads: List<Any?>?) {

        holder.onSetValues(manga)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is CatalogueItem) {
            return manga.id!! == other.manga.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id!!.hashCode()
    }



}