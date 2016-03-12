package eu.kanade.tachiyomi.ui.manga.myanimelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.card_myanimelist_personal.*
import kotlinx.android.synthetic.main.fragment_myanimelist.*
import nucleus.factory.RequiresPresenter
import java.text.DecimalFormat

@RequiresPresenter(MyAnimeListPresenter::class)
class MyAnimeListFragment : BaseRxFragment<MyAnimeListPresenter>() {

    companion object {
        fun newInstance(): MyAnimeListFragment {
            return MyAnimeListFragment()
        }
    }

    private var dialog: MyAnimeListDialogFragment? = null

    private val decimalFormat = DecimalFormat("#.##")

    private val SEARCH_FRAGMENT_TAG = "mal_search"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_myanimelist, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        swipe_refresh.isEnabled = false
        swipe_refresh.setOnRefreshListener { presenter.refresh() }
        myanimelist_title_layout.setOnClickListener { onTitleClick() }
        myanimelist_status_layout.setOnClickListener { onStatusClick() }
        myanimelist_chapters_layout.setOnClickListener { onChaptersClick() }
        myanimelist_score_layout.setOnClickListener { onScoreClick() }
    }

    fun setMangaSync(mangaSync: MangaSync?) {
        swipe_refresh.isEnabled = mangaSync != null
        mangaSync?.let {
            myanimelist_title.text = it.title
            val chaptersText = if (it.total_chapters > 0)
                "${it.last_chapter_read}/${it.total_chapters}" else "${it.last_chapter_read}/-"

            myanimelist_chapters.text = chaptersText
            myanimelist_score.text = if (it.score == 0f) "-" else decimalFormat.format(it.score)
            myanimelist_status.text = presenter.myAnimeList.getStatus(it.status)
        }
    }

    fun onRefreshDone() {
        swipe_refresh.isRefreshing = false
    }

    fun onRefreshError() {
        swipe_refresh.isRefreshing = false
    }

    fun setSearchResults(results: List<MangaSync>) {
        findSearchFragmentIfNeeded()

        dialog?.onSearchResults(results)
    }

    fun setSearchResultsError(error: Throwable) {
        findSearchFragmentIfNeeded()
        context.toast(error.message)

        dialog?.onSearchResultsError()
    }

    private fun findSearchFragmentIfNeeded() {
        if (dialog == null) {
            dialog = childFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG) as MyAnimeListDialogFragment
        }
    }

    fun onTitleClick() {
        if (dialog == null) {
            dialog = MyAnimeListDialogFragment.newInstance()
        }

        presenter.restartSearch()
        dialog?.show(childFragmentManager, SEARCH_FRAGMENT_TAG)
    }

    fun onStatusClick() {
        if (presenter.mangaSync == null)
            return

        MaterialDialog.Builder(activity)
                .title(R.string.status)
                .items(presenter.getAllStatus())
                .itemsCallbackSingleChoice(presenter.getIndexFromStatus(), { dialog, view, i, charSequence ->
                    presenter.setStatus(i)
                    myanimelist_status.text = "..."
                    true
                })
                .show()
    }

    fun onChaptersClick() {
        if (presenter.mangaSync == null)
            return

        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.chapters)
                .customView(R.layout.dialog_myanimelist_chapters, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { d, action ->
                    val view = d.customView
                    if (view != null) {
                        val np = view.findViewById(R.id.chapters_picker) as NumberPicker
                        np.clearFocus()
                        presenter.setLastChapterRead(np.value)
                        myanimelist_chapters.text = "..."
                    }
                }
                .show()

        val view = dialog.customView
        if (view != null) {
            val np = view.findViewById(R.id.chapters_picker) as NumberPicker
            // Set initial value
            np.value = presenter.mangaSync!!.last_chapter_read
            // Don't allow to go from 0 to 9999
            np.wrapSelectorWheel = false
        }
    }

    fun onScoreClick() {
        if (presenter.mangaSync == null)
            return

        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.score)
                .customView(R.layout.dialog_myanimelist_score, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { d, action ->
                    val view = d.customView
                    if (view != null) {
                        val np = view.findViewById(R.id.score_picker) as NumberPicker
                        np.clearFocus()
                        presenter.setScore(np.value)
                        myanimelist_score.text = "..."
                    }
                }
                .show()

        val view = dialog.customView
        if (view != null) {
            val np = view.findViewById(R.id.score_picker) as NumberPicker
            // Set initial value
            np.value = presenter.mangaSync!!.score.toInt()
        }
    }

}
