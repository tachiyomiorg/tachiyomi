package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MangaChaptersHeaderBinding
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks

class MangaChaptersHeaderAdapter(
    private val controller: MangaController
) :
    RecyclerView.Adapter<MangaChaptersHeaderAdapter.HeaderViewHolder>() {

    private var numChapters: Int? = null
    private var hasActiveFilters: Boolean = false

    private lateinit var binding: MangaChaptersHeaderBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = MangaChaptersHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding.root)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    fun setNumChapters(numChapters: Int) {
        this.numChapters = numChapters

        notifyDataSetChanged()
    }

    fun setHasActiveFilters(hasActiveFilters: Boolean) {
        this.hasActiveFilters = hasActiveFilters

        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            binding.chaptersLabel.text = if (numChapters == null) {
                view.context.getString(R.string.chapters)
            } else {
                view.context.resources.getQuantityString(R.plurals.manga_num_chapters, numChapters!!, numChapters)
            }

            val filterColor = if (hasActiveFilters) {
                view.context.getResourceColor(R.attr.colorFilterActive)
            } else {
                view.context.getResourceColor(R.attr.colorOnBackground)
            }
            DrawableCompat.setTint(binding.btnChaptersFilter.drawable, filterColor)

            merge(view.clicks(), binding.btnChaptersFilter.clicks())
                .onEach { controller.showSettingsSheet() }
                .launchIn(controller.viewScope)
        }
    }
}
