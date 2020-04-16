package eu.kanade.tachiyomi.ui.source

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.android.synthetic.main.source_main_controller_card.title

class LangHolder(view: View, adapter: FlexibleAdapter<*>) :
        BaseFlexibleViewHolder(view, adapter) {

    fun bind(item: LangItem) {
        title.text = LocaleHelper.getDisplayName(item.code, itemView.context)
    }
}
