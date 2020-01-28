package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.getResourceColor
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import uy.kohesive.injekt.injectLazy

class ChaptersAdapter(
        controller: ChaptersController,
        context: Context
) : FlexibleAdapter<ChapterItem>(null, controller, true) {

    val preferences: PreferencesHelper by injectLazy()

    var items: List<ChapterItem> = emptyList()

    val menuItemListener: OnMenuItemClickListener = controller

    val readColor = context.getResourceColor(android.R.attr.textColorHint)

    val unreadColor = context.getResourceColor(android.R.attr.textColorPrimary)

    val bookmarkedColor = context.getResourceColor(R.attr.colorAccent)

    val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols()
            .apply { decimalSeparator = '.' })

    val dateFormat: DateFormat = preferences.dateFormat().getOrDefault()

    override fun updateDataSet(items: List<ChapterItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun indexOf(item: ChapterItem): Int {
        return items.indexOf(item)
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(position: Int, item: MenuItem)
    }

}
