package eu.kanade.tachiyomi.ui.catalogue

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.LocaleHelper
import kotlinx.android.synthetic.main.catalogue_main_controller_card.*

class LangHolder(view: View, adapter: FlexibleAdapter<*>) :
        BaseFlexibleViewHolder(view, adapter) {

    fun bind(item: LangItem) {
        title.text = LocaleHelper.getDisplayName(item.code, itemView.context)
    }
}
