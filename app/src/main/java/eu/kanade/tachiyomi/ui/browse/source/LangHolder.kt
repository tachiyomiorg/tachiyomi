package eu.kanade.tachiyomi.ui.browse.source

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.databinding.SectionHeaderItemBinding
import eu.kanade.tachiyomi.util.system.LocaleHelper

class LangHolder(view: View, adapter: FlexibleAdapter<*>) :
    FlexibleViewHolder(view, adapter) {

    private val binding = SectionHeaderItemBinding.bind(view)

    fun bind(item: LangItem) {
        binding.title.text = LocaleHelper.getSourceDisplayName(item.code, itemView.context)
    }
}
