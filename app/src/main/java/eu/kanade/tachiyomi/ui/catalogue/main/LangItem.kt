package eu.kanade.tachiyomi.ui.catalogue.main

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.kanade.tachiyomi.R

/**
 * Item that contains the language header.
 *
 * @param code The lang code.
 */
data class LangItem(val code: String) : AbstractHeaderItem<LangHolder>() {

    /**
     * Returns the layout resource of this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card
    }

    /**
     * Creates a new view holder for this item.
     */
    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): LangHolder {

        return LangHolder(inflater.inflate(layoutRes, parent, false), adapter)
    }

    /**
     * Binds this item to the given view holder.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: LangHolder,
                                position: Int, payloads: List<Any?>?) {

        holder.bind(this)
    }

}
