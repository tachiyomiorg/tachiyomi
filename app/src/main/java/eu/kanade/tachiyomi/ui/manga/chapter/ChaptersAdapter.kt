package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChaptersAdapter
import eu.kanade.tachiyomi.util.system.getResourceColor
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class ChaptersAdapter(
    controller: MangaController,
    context: Context
) : BaseChaptersAdapter<ChapterItem>(controller) {

    private val preferences: PreferencesHelper by injectLazy()

    var items: List<ChapterItem> = emptyList()

    val readColor = context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    val unreadColor = context.getResourceColor(R.attr.colorOnSurface)
    val unreadColorSecondary = context.getResourceColor(android.R.attr.textColorSecondary)

    val bookmarkedColor = context.getResourceColor(R.attr.colorAccent)

    val decimalFormat = DecimalFormat(
        "#.###",
        DecimalFormatSymbols()
            .apply { decimalSeparator = '.' }
    )

    val dateFormat: DateFormat = preferences.dateFormat()

    override fun updateDataSet(items: List<ChapterItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun indexOf(item: ChapterItem): Int {
        return items.indexOf(item)
    }
}
