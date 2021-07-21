package eu.kanade.tachiyomi.ui.setting.database

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source

data class DatabaseSourceItem(val source: Source, val mangaCount: Int) :
    AbstractSectionableItem<DatabaseSourceHolder, AbstractHeaderItem<*>>(null) {

    override fun getLayoutRes(): Int {
        return R.layout.database_source_item
    }

    override fun createViewHolder(view: View?, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): DatabaseSourceHolder {
        return DatabaseSourceHolder(
            view!!,
            adapter as DatabaseSourceAdapter
        )
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: DatabaseSourceHolder, position: Int, payloads: MutableList<Any>?) {
        holder.bind(this)
    }
}
