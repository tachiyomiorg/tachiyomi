package eu.kanade.tachiyomi.ui.recent.manga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.NpaLinearLayoutManager
import kotlinx.android.synthetic.main.fragment_recent_manga.*
import nucleus.factory.RequiresPresenter

/**
 * TODO
 */
@RequiresPresenter(RecentMangaPresenter::class)
class RecentMangaFragment : BaseRxFragment<RecentMangaPresenter>() {
    companion object {
        /**
         * Create new RecentChaptersFragment.
         *
         */
        @JvmStatic
        fun newInstance(): RecentMangaFragment {
            return RecentMangaFragment()
        }
    }

    /**
     * Adapter containing the recent manga.
     */
    lateinit var adapter: RecentMangaAdapter
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
        recycler.layoutManager = NpaLinearLayoutManager(activity)
        adapter = RecentMangaAdapter(this)
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter

        // Update toolbar text
        setToolbarTitle(R.string.label_recent_manga)
    }

    /**
     * Populate adapter with chapters
     *
     * @param chapters list of chapters
     */
    fun onNextMangaChapters(chapters: List<Manga>) {
        (activity as MainActivity).updateEmptyView(chapters.isEmpty(),
                R.string.information_no_recent_manga, R.drawable.ic_glasses_black_128dp)

        adapter.setItems(chapters)
    }

    fun removeFromHistory(id: Long) {
        presenter.removeFromHistory(id)
        adapter.notifyDataSetChanged()
    }

    fun getNextUnreadChapter(manga: Manga): Chapter? {
        return presenter.getNextUnreadChapter(manga)
    }


    fun openChapter(chapter: Chapter?, manga: Manga) {
        if (chapter != null) {
            val intent = ReaderActivity.newIntent(activity, manga, chapter)
            startActivity(intent)
        } else {
            adapter.fragment.context.toast(R.string.no_next_chapter)
        }
    }

    fun openMangaInfo(manga: Manga) {
        val intent = MangaActivity.newIntent(activity, manga, true)
        startActivity(intent)
    }

    fun getLastRead(id: Long): String? {
        return presenter.getLastRead(id)
    }

}