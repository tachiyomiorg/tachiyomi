package eu.kanade.tachiyomi.ui.catalogue

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.item_catalogue_grid.view.*

class CatalogueItem(val manga: Manga) : AbstractFlexibleItem<CatalogueHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_catalogue_grid
    }

    override fun createViewHolder(adapter: FlexibleAdapter<IFlexible<*>>, inflater: LayoutInflater, parent: ViewGroup): CatalogueHolder {
        if (parent is AutofitRecyclerView) {
            val view = parent.inflate(R.layout.item_catalogue_grid).apply {
                card.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, parent.itemWidth / 3 * 4)
                gradient.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, parent.itemWidth / 3 * 4 / 2, Gravity.BOTTOM)
            }
            return CatalogueGridHolder(view, adapter)
        } else {
            val view = parent.inflate(R.layout.item_catalogue_list)
            return CatalogueListHolder(view, adapter)
        }
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>>, holder: CatalogueHolder, position: Int, payloads: List<Any?>?) {
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