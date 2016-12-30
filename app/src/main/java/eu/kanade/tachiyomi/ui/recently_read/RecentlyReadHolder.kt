package eu.kanade.tachiyomi.ui.recently_read

import android.support.v7.widget.RecyclerView
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.widget.DialogCheckboxView
import kotlinx.android.synthetic.main.item_recently_read.view.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * Holder that contains recent manga item
 * Uses R.layout.item_recently_read.
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new recent chapter holder.
 */
class RecentlyReadHolder(view: View, private val adapter: RecentlyReadAdapter)
    : RecyclerView.ViewHolder(view) {

    /**
     * DecimalFormat used to display correct chapter number
     */
    private val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })

    private val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    /**
     * Set values of view
     *
     * @param item item containing history information
     */
    fun onSetValues(item: MangaChapterHistory) {
        // Retrieve objects
        val manga = item.manga
        val chapter = item.chapter
        val history = item.history

        // Set manga title
        itemView.manga_title.text = manga.title

        // Set source + chapter title
        val formattedNumber = decimalFormat.format(chapter.chapter_number.toDouble())
        itemView.manga_source.text = itemView.context.getString(R.string.recent_manga_source)
                .format(adapter.sourceManager.get(manga.source)?.name, formattedNumber)

        // Set last read timestamp title
        itemView.last_read.text = df.format(Date(history.last_read))

        // Set cover
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(itemView.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(itemView.cover)
        }

        // Set remove clickListener
        itemView.remove.setOnClickListener {
            // Create custom view
            val dialogCheckboxView = DialogCheckboxView(itemView.context).apply {
                setDescription(R.string.dialog_with_checkbox_remove_description)
                setOptionDescription(R.string.dialog_with_checkbox_reset)
            }
            MaterialDialog.Builder(itemView.context)
                    .title(R.string.action_remove)
                    .customView(dialogCheckboxView, true)
                    .positiveText(R.string.action_remove)
                    .negativeText(android.R.string.cancel)
                    .onPositive { materialDialog, dialogAction ->
                        // Check if user wants all chapters reset
                        if (dialogCheckboxView.isChecked()) {
                            adapter.fragment.removeAllFromHistory(manga.id!!)
                        } else {
                            adapter.fragment.removeFromHistory(history)
                        }
                    }
                    .onNegative { materialDialog, dialogAction ->
                        materialDialog.dismiss()
                    }.show()
        }

        // Set continue reading clickListener
        itemView.resume.setOnClickListener {
            adapter.fragment.openChapter(chapter, manga)
        }

        // Set open manga info clickListener
        itemView.cover.setOnClickListener {
            adapter.fragment.openMangaInfo(manga)
        }
    }

}
