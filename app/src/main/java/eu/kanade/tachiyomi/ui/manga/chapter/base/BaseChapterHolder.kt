package eu.kanade.tachiyomi.ui.manga.chapter.base

import android.view.View
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.view.popupMenu

open class BaseChapterHolder(
    view: View,
    private val adapter: BaseChaptersAdapter<*>
) : FlexibleViewHolder(view, adapter) {

    fun onDownloadClick(view: View, position: Int) {
        val item = adapter.getItem(position) as? BaseChapterItem<*, *> ?: return
        when (item.status) {
            Download.State.NOT_DOWNLOADED, Download.State.ERROR -> {
                adapter.clickListener.downloadChapter(position)
            }
            else -> {
                view.popupMenu(
                    R.menu.chapter_download,
                    initMenu = {
                        // Download.State.DOWNLOADED
                        findItem(R.id.delete_download).isVisible = item.status == Download.State.DOWNLOADED

                        // Download.State.DOWNLOADING, Download.State.QUEUE
                        findItem(R.id.cancel_download).isVisible = item.status != Download.State.DOWNLOADED

                        // Download.State.QUEUE
                        findItem(R.id.start_download).isVisible = item.status == Download.State.QUEUE
                    },
                    onMenuItemClick = {
                        if (itemId == R.id.start_download) {
                            adapter.clickListener.startDownloadNow(position)
                        } else {
                            adapter.clickListener.deleteChapter(position)
                        }
                    }
                )
            }
        }
    }
}
