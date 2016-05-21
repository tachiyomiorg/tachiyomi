package eu.kanade.tachiyomi.ui.recent_updates

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import kotlinx.android.synthetic.main.item_recent_chapter_section.view.*
import java.util.*

class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    /**
     * Current date
     */
    private val now = Date().time

    /**
     * Set value of section header
     *
     * @param date of section header
     */
    fun onSetValues(date: Date) {
        val s = DateUtils.getRelativeTimeSpanString(
                date.time, now, DateUtils.DAY_IN_MILLIS)
        itemView.section_text.text = s
    }
}