package eu.kanade.tachiyomi.ui.extension

import android.annotation.SuppressLint
import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.extension_card_header.*

class ExtensionGroupHolder(view: View, adapter: FlexibleAdapter<*>) :
        BaseFlexibleViewHolder(view, adapter) {

    @SuppressLint("SetTextI18n")
    fun bind(item: ExtensionGroupItem) {
        title.text = when {
            item.installed -> itemView.context.getString(R.string.ext_installed)
            else -> itemView.context.getString(R.string.ext_available)
        } + " (" + item.size + ")"
    }
}
