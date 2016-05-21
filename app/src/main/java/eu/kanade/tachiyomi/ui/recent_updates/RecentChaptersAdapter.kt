package eu.kanade.tachiyomi.ui.recent_updates

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.util.inflate
import java.util.*

/**
 * Adapter of RecentChaptersHolder.
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param fragment a RecentChaptersFragment object
 * @constructor creates an instance of the adapter.
 */

class RecentChaptersAdapter(val fragment: RecentChaptersFragment) : FlexibleAdapter<RecyclerView.ViewHolder, Any>() {
    /**
     * The id of the view type
     */
    private val VIEW_TYPE_CHAPTER = 0

    /**
     * The id of the view type
     */
    private val VIEW_TYPE_SECTION = 1

    init {
        // Let each each item in the data set be represented with a unique identifier.
        setHasStableIds(true)
    }

    /**
     * Called when ViewHolder is bind
     *
     * @param holder bind holder
     * @param position position of holder
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Check which view type and set correct values.
        val item = getItem(position)
        when (holder.itemViewType) {
            VIEW_TYPE_CHAPTER -> {
                if (item is MangaChapter) {
                    (holder as RecentChaptersHolder).onSetValues(item)
                }
            }
            VIEW_TYPE_SECTION -> {
                if (item is Date) {
                    (holder as SectionViewHolder).onSetValues(item)
                }
            }
        }

        //When user scrolls this bind the correct selection status
        holder.itemView.isActivated = isSelected(position)
    }

    /**
     * Called when ViewHolder is created
     *
     * @param parent parent View
     * @param viewType int containing viewType
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        val view: View

        // Check which view type and set correct values.
        when (viewType) {
            VIEW_TYPE_CHAPTER -> {
                view = parent.inflate(R.layout.item_recent_chapters)
                return RecentChaptersHolder(view, this, fragment)
            }
            VIEW_TYPE_SECTION -> {
                view = parent.inflate(R.layout.item_recent_chapter_section)
                return SectionViewHolder(view)
            }
        }
        return null
    }

    /**
     * Returns the correct ViewType
     *
     * @param position position of item
     */
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is MangaChapter) VIEW_TYPE_CHAPTER else VIEW_TYPE_SECTION
    }


    /**
     * Update items

     * @param items items
     */
    fun setItems(items: List<Any>) {
        mItems = items
        notifyDataSetChanged()
    }

    /**
     * Needed to determine holder id
     *
     * @param position position of holder item
     */
    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        if (item is MangaChapter)
            return item.chapter.id
        else
            return item.hashCode().toLong()
    }

    /**
     * Abstract function (not needed).
     *
     * @param p0 a string.
     */
    override fun updateDataSet(p0: String) {
        // Empty function.
    }

}