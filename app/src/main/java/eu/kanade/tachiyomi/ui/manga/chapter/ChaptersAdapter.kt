package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.util.inflate

class ChaptersAdapter(val fragment: ChaptersFragment) : FlexibleAdapter<ChaptersHolder, Chapter>() {

    init {
        setHasStableIds(true)
    }

    override fun updateDataSet(param: String) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChaptersHolder {
        val v = parent.inflate(R.layout.item_chapter)
        return ChaptersHolder(v, this, fragment)
    }

    override fun onBindViewHolder(holder: ChaptersHolder, position: Int) {
        val chapter = getItem(position)
        val manga = fragment.presenter.manga
        holder.onSetValues(chapter, manga)

        //When user scrolls this bind the correct selection status
        holder.itemView.isActivated = isSelected(position)
    }

    override fun getItemId(position: Int): Long {
        return mItems[position].id
    }

    fun setItems(chapters: List<Chapter>) {
        mItems = chapters
        notifyDataSetChanged()
    }
}
