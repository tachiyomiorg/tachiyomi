package eu.kanade.tachiyomi.ui.recently_read

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.widget.NpaLinearLayoutManager
import kotlinx.android.synthetic.main.fragment_recent_manga.*
import nucleus.factory.RequiresPresenter

/**
 * Fragment that shows recently read manga.
 * Uses R.layout.fragment_recent_manga.
 * UI related actions should be called from here.
 */
@RequiresPresenter(RecentlyReadPresenter::class)
class RecentlyReadFragment : BaseRxFragment<RecentlyReadPresenter>() {
    companion object {
        /**
         * Create new RecentChaptersFragment.
         *
         */
        @JvmStatic
        fun newInstance(): RecentlyReadFragment {
            return RecentlyReadFragment()
        }
    }

    /**
     * Adapter containing the recent manga.
     */
    lateinit var adapter: RecentlyReadAdapter
        private set

    /**
     * Called when view gets created
     *
     * @param inflater layout inflater
     * @param container view group
     * @param savedState status of saved state
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recent_manga, container, false)
    }

    /**
     * Called when view is created
     *
     * @param view created view
     * @param savedInstanceState status of saved sate
     */
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        // Initialize adapter
        recycler.layoutManager = NpaLinearLayoutManager(activity)
        adapter = RecentlyReadAdapter(this)
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter

        // Update toolbar text
        setToolbarTitle(R.string.label_recent_manga)
    }

    /**
     * Populate adapter with chapters
     *
     * @param mangaHistory list of manga history
     */
    fun onNextManga(mangaHistory: List<MangaChapterHistory>) {
        (activity as MainActivity).updateEmptyView(mangaHistory.isEmpty(),
                R.string.information_no_recent_manga, R.drawable.ic_glasses_black_128dp)

        adapter.setItems(mangaHistory)
    }

    /**
     * Reset last read of chapter to 0L
     * @param history history belonging to chapter
     */
    fun removeFromHistory(history: History) {
        presenter.removeFromHistory(history)
        adapter.notifyDataSetChanged()
    }

    /**
     * Removes all chapters belonging to manga from library
     * @param mangaId id of manga
     */
    fun removeAllFromHistory(mangaId: Long) {
        presenter.removeAllFromHistory(mangaId)
        adapter.notifyDataSetChanged()
    }

    /**
     * Open chapter to continue reading
     * @param chapter chapter that is opened
     * @param manga manga belonging to chapter
     */
    fun openChapter(chapter: Chapter, manga: Manga) {
        val intent = ReaderActivity.newIntent(activity, manga, chapter)
        startActivity(intent)
    }

    /**
     * Open manga info page
     * @param manga manga belonging to info page
     */
    fun openMangaInfo(manga: Manga) {
        val intent = MangaActivity.newIntent(activity, manga, true)
        startActivity(intent)
    }

    /**
     * Returns the timestamp of last read
     * @param history history containing time of last read
     */
    fun getLastRead(history: History): String? {
        return presenter.getLastRead(history)
    }

}