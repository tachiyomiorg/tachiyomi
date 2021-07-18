package eu.kanade.tachiyomi.ui.library

import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tfcporciuncula.flow.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.databinding.SourceCompactGridItemBinding
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.library.setting.DisplayModeSetting
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryItem(
    val manga: LibraryManga,
    private val shouldSetFromCategory: Preference<Boolean>,
    private val defaultLibraryDisplayMode: Preference<DisplayModeSetting>
) :
    AbstractFlexibleItem<LibraryHolder<*>>(), IFilterable<String> {

    private val sourceManager: SourceManager = Injekt.get()

    var displayMode: Int = -1
    var downloadCount = -1
    var unreadCount = -1
    var isLocal = false

    private fun getDisplayMode(): DisplayModeSetting {
        return if (shouldSetFromCategory.get() && manga.category != 0) {
            DisplayModeSetting.fromFlag(displayMode)
        } else {
            defaultLibraryDisplayMode.get()
        }
    }

    override fun getLayoutRes(): Int {
        return when (getDisplayMode()) {
            DisplayModeSetting.COMPACT_GRID -> R.layout.source_compact_grid_item
            DisplayModeSetting.COMFORTABLE_GRID -> R.layout.source_comfortable_grid_item
            DisplayModeSetting.LIST -> R.layout.source_list_item
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): LibraryHolder<*> {
        return when (getDisplayMode()) {
            DisplayModeSetting.COMPACT_GRID -> {
                val binding = SourceCompactGridItemBinding.bind(view)
                val parent = adapter.recyclerView as AutofitRecyclerView
                val coverHeight = parent.itemWidth / 3 * 4
                view.apply {
                    binding.card.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight)
                    binding.gradient.layoutParams = FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        coverHeight / 2,
                        Gravity.BOTTOM
                    )
                }
                LibraryCompactGridHolder(view, adapter)
            }
            DisplayModeSetting.COMFORTABLE_GRID -> {
                val binding = SourceComfortableGridItemBinding.bind(view)
                val parent = adapter.recyclerView as AutofitRecyclerView
                val coverHeight = parent.itemWidth / 3 * 4
                view.apply {
                    binding.card.layoutParams = ConstraintLayout.LayoutParams(
                        MATCH_PARENT,
                        coverHeight
                    )
                }
                LibraryComfortableGridHolder(view, adapter)
            }
            DisplayModeSetting.LIST -> {
                LibraryListHolder(view, adapter)
            }
        }
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: LibraryHolder<*>,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.onSetValues(this)
    }

    /**
     * Filters a manga depending on a query.
     *
     * @param constraint the query to apply.
     * @return true if the manga should be included, false otherwise.
     */
    override fun filter(constraint: String): Boolean {
        val sourceName by lazy { sourceManager.getOrStub(manga.source).name }
        val genres by lazy { manga.getGenres() }
        return manga.title.contains(constraint, true) ||
            (manga.author?.contains(constraint, true) ?: false) ||
            (manga.artist?.contains(constraint, true) ?: false) ||
            (manga.description?.contains(constraint, true) ?: false) ||
            if (constraint.contains(",")) {
                constraint.split(",").all { containsSourceOrGenre(it.trim(), sourceName, genres) }
            } else {
                containsSourceOrGenre(constraint, sourceName, genres)
            }
    }

    /**
     * Filters a manga by checking whether the query is the manga's source OR part of
     * the genres of the manga
     * Checking for genre is done only if the query isn't part of the source name.
     *
     * @param query the query to check
     * @param sourceName name of the manga's source
     * @param genres list containing manga's genres
     */
    private fun containsSourceOrGenre(query: String, sourceName: String, genres: List<String>?): Boolean {
        val minus = query.startsWith("-")
        val tag = if (minus) { query.substringAfter("-") } else query
        return when (sourceName.contains(tag, true)) {
            false -> containsGenre(query, genres)
            else -> !minus
        }
    }

    private fun containsGenre(tag: String, genres: List<String>?): Boolean {
        return if (tag.startsWith("-")) {
            genres?.find {
                it.trim().equals(tag.substringAfter("-"), ignoreCase = true)
            } == null
        } else {
            genres?.find {
                it.trim().equals(tag, ignoreCase = true)
            } != null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is LibraryItem) {
            return manga.id == other.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id!!.hashCode()
    }
}
