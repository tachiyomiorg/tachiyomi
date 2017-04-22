package eu.kanade.tachiyomi.ui.recently_read

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.manga2.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_recently_read.view.*

/**
 * Fragment that shows recently read manga.
 * Uses R.layout.fragment_recently_read.
 * UI related actions should be called from here.
 */
class RecentlyReadController : NucleusController<RecentlyReadPresenter>(),
        RecentlyReadAdapter.OnRemoveClickListener,
        RecentlyReadAdapter.OnResumeClickListener,
        RecentlyReadAdapter.OnCoverClickListener,
        RemoveHistoryDialog.Listener {

    /**
     * Adapter containing the recent manga.
     */
    var adapter: RecentlyReadAdapter? = null
        private set

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chapters, menu)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_recent_manga)
    }

    override fun createPresenter(): RecentlyReadPresenter {
        return RecentlyReadPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.fragment_recently_read, container, false)
    }

    /**
     * Called when view is created
     *
     * @param view created view
     * @param savedViewState saved state of the view
     */
    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        with(view) {
            // Initialize adapter
            recycler.layoutManager = LinearLayoutManager(context)
            adapter = RecentlyReadAdapter(this@RecentlyReadController)
            recycler.setHasFixedSize(true)
            recycler.adapter = adapter
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
    }

    /**
     * Populate adapter with chapters
     *
     * @param mangaHistory list of manga history
     */
    fun onNextManga(mangaHistory: List<RecentlyReadItem>) {
//        (activity as MainActivity2).updateEmptyView(mangaHistory.isEmpty(),
//                R.string.information_no_recent_manga, R.drawable.ic_glasses_black_128dp)

        adapter?.updateDataSet(mangaHistory.toList())
    }

    /**
     * Called from the presenter when wanting to open the next chapter of the current one.
     *
     * @param chapter the next chapter or null if it doesn't exist.
     * @param manga the manga of the chapter.
     */
    fun onOpenNextChapter(chapter: Chapter?, manga: Manga) {
        val activity = activity ?: return
        if (chapter == null) {
            activity.toast(R.string.no_next_chapter)
            return
        }

        val intent = ReaderActivity.newIntent(activity, manga, chapter)
        startActivity(intent)
    }

    override fun onResumeClick(position: Int) {
        val activity = activity ?: return // TODO probably not needed in the future
        val adapter = adapter ?: return
        if (position == RecyclerView.NO_POSITION) return

        val (manga, chapter, _) = adapter.getItem(position).mch

        // TODO single presenter call
        if (!chapter.read) {
            val intent = ReaderActivity.newIntent(activity, manga, chapter)
            startActivity(intent)
        } else {
            presenter.openNextChapter(chapter, manga)
        }
    }

    override fun onRemoveClick(position: Int) {
        val adapter = adapter ?: return
        if (position == RecyclerView.NO_POSITION) return

        val (manga, _, history) = adapter.getItem(position).mch

        RemoveHistoryDialog(this, manga, history).showDialog(router)
    }

    override fun onCoverClick(position: Int) {
        val manga = adapter?.getItem(position)?.mch?.manga ?: return
        router.pushController(RouterTransaction.with(MangaController(manga))
                .pushChangeHandler(FadeChangeHandler())
                .popChangeHandler(FadeChangeHandler()))
    }

    override fun removeHistory(manga: Manga, history: History, all: Boolean) {
        if (all) {
            // Reset last read of chapter to 0L
            presenter.removeAllFromHistory(manga.id!!)
        } else {
            // Remove all chapters belonging to manga from library
            presenter.removeFromHistory(history)
        }
    }

}