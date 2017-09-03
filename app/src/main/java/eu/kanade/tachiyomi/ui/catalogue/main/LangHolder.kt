package eu.kanade.tachiyomi.ui.catalogue.main

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.catalogue_main_controller_card.view.*
import java.util.*

class LangHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter, true) {

    fun bind(item: LangItem) {
        itemView.title.text = when {
            item.code == "" -> itemView.context.getString(R.string.other_source)
            else -> {
                val locale = Locale(item.code)
                locale.getDisplayName(locale).capitalize()
            }
        }
    }
}