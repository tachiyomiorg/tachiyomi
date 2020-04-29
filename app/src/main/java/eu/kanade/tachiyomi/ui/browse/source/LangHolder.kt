package eu.kanade.tachiyomi.ui.browse.source

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.android.synthetic.main.source_main_controller_card_header.title

class LangHolder(view: View, adapter: FlexibleAdapter<*>) :
    BaseFlexibleViewHolder(view, adapter) {

    fun bind(item: LangItem) {
        title.text = LocaleHelper.getSourceDisplayName(item.code, itemView.context)
    }
}
