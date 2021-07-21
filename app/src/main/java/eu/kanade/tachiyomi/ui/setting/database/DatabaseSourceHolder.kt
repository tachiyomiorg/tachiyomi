package eu.kanade.tachiyomi.ui.setting.database

import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DatabaseSourceItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.util.system.LocaleHelper

class DatabaseSourceHolder(view: View, val adapter: DatabaseSourceAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = DatabaseSourceItemBinding.bind(view)

    fun bind(item: DatabaseSourceItem) {
        binding.title.text = if (item.source.id != LocalSource.ID) {
            "${item.source.name} (${LocaleHelper.getSourceDisplayName(item.source.lang, itemView.context)})"
        } else item.source.name

        binding.description.text = itemView.context.getString(R.string.database_source_manga_count, item.mangaCount)

        binding.checkbox.isChecked = adapter.isSelected(bindingAdapterPosition)
        itemView.post {
            when {
                item.source.id == LocalSource.ID -> binding.thumbnail.setImageResource(R.mipmap.ic_local_source)
                item.source.icon() != null -> binding.thumbnail.setImageDrawable(item.source.icon())
            }
        }
    }
}
