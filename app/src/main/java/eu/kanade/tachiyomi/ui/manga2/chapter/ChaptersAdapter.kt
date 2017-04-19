package eu.kanade.tachiyomi.ui.manga2.chapter

import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter

class ChaptersAdapter(val fragment: ChaptersFragment) : FlexibleAdapter<ChapterItem>(null, fragment, true) {

    var items: List<ChapterItem> = emptyList()

    val menuItemListener: (Int, MenuItem) -> Unit = { position, item ->
        fragment.onItemMenuClick(position, item)
    }

    override fun updateDataSet(items: List<ChapterItem>) {
        this.items = items
        super.updateDataSet(items.toList())
    }

}
