package eu.kanade.tachiyomi.ui.recent.manga

import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.SourceManager
import kotlinx.android.synthetic.main.item_recent_manga.view.*

class RecentMangaHolder(view: View, private val adapter: RecentMangaAdapter) :
        RecyclerView.ViewHolder(view) {


    fun onSetValues(manga: Manga) {
        // Set manga title
        itemView.manga_title.text = manga.title
        itemView.manga_source.text = SourceManager(adapter.fragment.context).get(manga.source)?.visibleName
        itemView.last_read.text = adapter.fragment.getLastRead(manga.id)

        itemView.remove.setOnClickListener {
            adapter.fragment.removeFromHistory(manga.id)
        }

        itemView.continue_reading.setOnClickListener {
            val chapter = adapter.fragment.getNextUnreadChapter(manga)
            adapter.fragment.openChapter(chapter, manga)
        }

        itemView.cover.setOnClickListener {
            adapter.fragment.openMangaInfo(manga)
        }

        // Set cover
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(itemView.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(itemView.cover)
        }
    }
}
