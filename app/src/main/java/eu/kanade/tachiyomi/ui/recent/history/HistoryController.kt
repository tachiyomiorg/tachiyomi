package eu.kanade.tachiyomi.ui.recent.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.HistoryControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NoToolbarElevationController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast

/**
 * Fragment that shows recently read manga.
 * Uses [R.layout.history_controller].
 * UI related actions should be called from here.
 */
class HistoryController :
    NucleusController<HistoryControllerBinding, HistoryPresenter>(),
    RootController,
    NoToolbarElevationController,
    FlexibleAdapter.OnUpdateListener,
    HistoryAdapter.OnRemoveClickListener,
    HistoryAdapter.OnResumeClickListener,
    HistoryAdapter.OnItemClickListener,
    RemoveHistoryDialog.Listener {

    /**
     * Adapter containing the recent manga.
     */
    var adapter: HistoryAdapter? = null
        private set

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_recent_manga)
    }

    override fun createPresenter(): HistoryPresenter {
        return HistoryPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = HistoryControllerBinding.inflate(inflater)
        return binding.root
    }

    /**
     * Called when view is created
     *
     * @param view created view
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize adapter
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        adapter = HistoryAdapter(this@HistoryController)
        binding.recycler.setHasFixedSize(true)
        binding.recycler.adapter = adapter
        adapter?.fastScroller = binding.fastScroller
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    /**
     * Populate adapter with chapters
     *
     * @param mangaHistory list of manga history
     */
    fun onNextManga(mangaHistory: List<HistoryItem>) {
        adapter?.updateDataSet(mangaHistory)
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(R.string.information_no_recent_manga)
        }
    }

    override fun onResumeClick(position: Int) {
        val activity = activity ?: return
        val (manga, chapter, _) = adapter?.getItem(position)?.mch ?: return

        val nextChapter = presenter.getNextChapter(chapter, manga)
        if (nextChapter != null) {
            val intent = ReaderActivity.newIntent(activity, manga, nextChapter)
            startActivity(intent)
        } else {
            activity.toast(R.string.no_next_chapter)
        }
    }

    override fun onRemoveClick(position: Int) {
        val (manga, _, history) = adapter?.getItem(position)?.mch ?: return
        RemoveHistoryDialog(this, manga, history).showDialog(router)
    }

    override fun onItemClick(position: Int) {
        val manga = adapter?.getItem(position)?.mch?.manga ?: return
        router.pushController(MangaController(manga).withFadeTransaction())
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
