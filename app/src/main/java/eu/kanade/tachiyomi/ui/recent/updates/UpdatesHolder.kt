package eu.kanade.tachiyomi.ui.recent.updates

import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.databinding.UpdatesItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.manga.chapter.base.BaseChapterHolder

/**
 * Holder that contains chapter item
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new recent chapter holder.
 */
class UpdatesHolder(private val view: View, private val adapter: UpdatesAdapter) :
    BaseChapterHolder(view, adapter) {

    private val binding = UpdatesItemBinding.bind(view)

    init {
        binding.mangaCover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }

        binding.download.setOnClickListener { onDownloadClick(it) }
    }

    fun bind(item: UpdatesItem) {
        // Set chapter title
        binding.chapterTitle.text = item.chapter.name

        // Set manga title
        binding.mangaTitle.text = item.manga.title

        // Check if chapter is read and set correct color
        if (item.chapter.read) {
            binding.chapterTitle.setTextColor(adapter.readColor)
            binding.mangaTitle.setTextColor(adapter.readColor)
        } else {
            binding.chapterTitle.setTextColor(adapter.unreadColor)
            binding.mangaTitle.setTextColor(adapter.unreadColor)
        }

        // Set chapter status
        binding.download.isVisible = item.manga.source != LocalSource.ID
        binding.download.setState(item.status, item.progress)

        // Set cover
        GlideApp.with(itemView.context).clear(binding.mangaCover)

        val radius = itemView.context.resources.getDimensionPixelSize(R.dimen.card_radius)
        val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(radius))
        GlideApp.with(itemView.context)
            .load(item.manga.toMangaThumbnail())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .apply(requestOptions)
            .dontAnimate()
            .into(binding.mangaCover)
    }
}
