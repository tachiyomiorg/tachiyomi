package eu.kanade.tachiyomi.ui.catalogue.main

import android.view.LayoutInflater
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.kanade.tachiyomi.R

class LangItem(val code: String) : AbstractHeaderItem<LangHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.catalogue_main_controller_card
    }

    override fun createViewHolder(adapter: FlexibleAdapter<*>, inflater: LayoutInflater,
                                  parent: ViewGroup): LangHolder {

        return LangHolder(inflater.inflate(layoutRes, parent, false), adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: LangHolder,
                                position: Int, payloads: List<Any?>?) {

        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is LangItem -> code == other.code
            else -> false
        }
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

}
