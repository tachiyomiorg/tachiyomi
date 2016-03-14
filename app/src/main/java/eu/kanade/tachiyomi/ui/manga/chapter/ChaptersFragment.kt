package eu.kanade.tachiyomi.ui.manga.chapter

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.decoration.DividerItemDecoration
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.getResourceDrawable
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_manga_chapters.*
import nucleus.factory.RequiresPresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

@RequiresPresenter(ChaptersPresenter::class)
class ChaptersFragment : BaseRxFragment<ChaptersPresenter>(), ActionMode.Callback, FlexibleViewHolder.OnListItemClickListener {

    companion object {
        /**
         * Creates a new instance of this fragment.
         *
         * @return a new instance of [ChaptersFragment].
         */
        fun newInstance(): ChaptersFragment {
            return ChaptersFragment()
        }
    }

    /**
     * Adapter containing a list of chapters.
     */
    private lateinit var adapter: ChaptersAdapter

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manga_chapters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Init RecyclerView and adapter
        adapter = ChaptersAdapter(this)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.addItemDecoration(DividerItemDecoration(
                context.theme.getResourceDrawable(R.attr.divider_drawable)))
        recycler.setHasFixedSize(true)

        swipe_refresh.setOnRefreshListener { fetchChapters() }

        next_unread_btn.setOnClickListener { v ->
            val chapter = presenter.getNextUnreadChapter()
            if (chapter != null) {
                openChapter(chapter)
            } else {
                context.toast(R.string.no_next_chapter)
            }
        }

    }

    override fun onPause() {
        // Stop recycler's scrolling when onPause is called. If the activity is finishing
        // the presenter will be destroyed, and it could cause NPE
        // https://github.com/inorichi/tachiyomi/issues/159
        recycler.stopScroll()

        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chapters, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_display_mode -> showDisplayModeDialog()
            R.id.manga_download -> showDownloadDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun onNextManga(manga: Manga) {
        // Remove listeners before setting the values
        show_unread.setOnCheckedChangeListener(null)
        show_downloaded.setOnCheckedChangeListener(null)
        sort_btn.setOnClickListener(null)

        // Set initial values
        setReadFilter()
        setDownloadedFilter()
        setSortIcon()

        // Init listeners
        show_unread.setOnCheckedChangeListener { arg, isChecked -> presenter.setReadFilter(isChecked) }
        show_downloaded.setOnCheckedChangeListener { v, isChecked -> presenter.setDownloadedFilter(isChecked) }
        sort_btn.setOnClickListener {
            presenter.revertSortOrder()
            setSortIcon()
        }
    }

    fun onNextChapters(chapters: List<Chapter>) {
        // If the list is empty, fetch chapters from source if the conditions are met
        // We use presenter chapters instead because they are always unfiltered
        if (presenter.chapters.isEmpty())
            initialFetchChapters()

        destroyActionModeIfNeeded()
        adapter.setItems(chapters)
    }

    private fun initialFetchChapters() {
        // Only fetch if this view is from the catalog and it hasn't requested previously
        if (isCatalogueManga && !presenter.hasRequested) {
            fetchChapters()
        }
    }

    fun fetchChapters() {
        swipe_refresh.isRefreshing = true
        presenter.fetchChaptersFromSource()
    }

    fun onFetchChaptersDone() {
        swipe_refresh.isRefreshing = false
    }

    fun onFetchChaptersError(error: Throwable) {
        swipe_refresh.isRefreshing = false
        context.toast(error.message)
    }

    val isCatalogueManga: Boolean
        get() = (activity as MangaActivity).isCatalogueManga

    protected fun openChapter(chapter: Chapter) {
        presenter.onOpenChapter(chapter)
        val intent = ReaderActivity.newIntent(activity)
        startActivity(intent)
    }

    private fun showDisplayModeDialog() {

        // Get available modes, ids and the selected mode
        val modes = listOf(getString(R.string.show_title), getString(R.string.show_chapter_number))
        val ids = intArrayOf(Manga.DISPLAY_NAME, Manga.DISPLAY_NUMBER)
        val selectedIndex = if (presenter.manga.displayMode == Manga.DISPLAY_NAME) 0 else 1

        MaterialDialog.Builder(activity)
                .title(R.string.action_display_mode)
                .items(modes)
                .itemsIds(ids)
                .itemsCallbackSingleChoice(selectedIndex) { dialog, itemView, which, text ->
                    // Save the new display mode
                    presenter.setDisplayMode(itemView.id)
                    // Refresh ui
                    adapter.notifyDataSetChanged()
                    true
                }
                .show()
    }

    private fun showDownloadDialog() {
        // Get available modes
        val modes = listOf(getString(R.string.download_all), getString(R.string.download_unread))

        MaterialDialog.Builder(activity)
                .title(R.string.manga_download)
                .negativeText(android.R.string.cancel)
                .items(modes)
                .itemsCallback { dialog, view, i, charSequence ->
                    val chapters = ArrayList<Chapter>()

                    for (chapter in presenter.chapters) {
                        if (!chapter.isDownloaded) {
                            if (i == 0 || (i == 1 && !chapter.read)) {
                                chapters.add(chapter)
                            }
                        }
                    }
                    if (chapters.size > 0) {
                        onDownload(Observable.from(chapters))
                    }
                }
                .show()
    }

    fun onChapterStatusChange(download: Download) {
        getHolder(download.chapter)?.notifyStatus(download.status)
    }

    private fun getHolder(chapter: Chapter): ChaptersHolder? {
        return recycler.findViewHolderForItemId(chapter.id) as? ChaptersHolder
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.chapter_selection, menu)
        adapter.mode = FlexibleAdapter.MODE_MULTI
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> onSelectAll()
            R.id.action_mark_as_read -> onMarkAsRead(getSelectedChapters())
            R.id.action_mark_as_unread -> onMarkAsUnread(getSelectedChapters())
            R.id.action_download -> onDownload(getSelectedChapters())
            R.id.action_delete -> onDelete(getSelectedChapters())
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        adapter.mode = FlexibleAdapter.MODE_SINGLE
        adapter.clearSelection()
        actionMode = null
    }

    fun getSelectedChapters(): Observable<Chapter> {
        val chapters = adapter.selectedItems.map { adapter.getItem(it) }
        return Observable.from(chapters)
    }

    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    protected fun onSelectAll() {
        adapter.selectAll()
        setContextTitle(adapter.selectedItemCount)
    }

    fun onMarkAsRead(chapters: Observable<Chapter>) {
        presenter.markChaptersRead(chapters, true)
    }

    fun onMarkAsUnread(chapters: Observable<Chapter>) {
        presenter.markChaptersRead(chapters, false)
    }

    fun onMarkPreviousAsRead(chapter: Chapter) {
        presenter.markPreviousChaptersAsRead(chapter)
    }

    fun onDownload(chapters: Observable<Chapter>) {
        DownloadService.start(activity)

        val observable = chapters.doOnCompleted { adapter.notifyDataSetChanged() }

        presenter.downloadChapters(observable)
        destroyActionModeIfNeeded()
    }

    fun onDelete(chapters: Observable<Chapter>) {
        val size = adapter.selectedItemCount

        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.deleting)
                .progress(false, size, true)
                .cancelable(false)
                .show()

        val observable = chapters
                .concatMap { chapter ->
                    presenter.deleteChapter(chapter)
                    Observable.just(chapter)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { chapter ->
                    dialog.incrementProgress(1)
                    chapter.status = Download.NOT_DOWNLOADED
                }
                .doOnCompleted { adapter.notifyDataSetChanged() }
                .doAfterTerminate { dialog.dismiss() }

        presenter.deleteChapters(observable)
        destroyActionModeIfNeeded()
    }

    override fun onListItemClick(position: Int): Boolean {
        if (actionMode != null && adapter.mode == FlexibleAdapter.MODE_MULTI) {
            toggleSelection(position)
            return true
        } else {
            openChapter(adapter.getItem(position))
            return false
        }
    }

    override fun onListItemLongClick(position: Int) {
        if (actionMode == null)
            actionMode = baseActivity.startSupportActionMode(this)

        toggleSelection(position)
    }

    private fun toggleSelection(position: Int) {
        adapter.toggleSelection(position, false)

        val count = adapter.selectedItemCount
        if (count == 0) {
            actionMode?.finish()
        } else {
            setContextTitle(count)
            actionMode?.invalidate()
        }
    }

    private fun setContextTitle(count: Int) {
        actionMode?.title = getString(R.string.label_selected, count)
    }

    fun setSortIcon() {
        sort_btn?.let {
            val aToZ = presenter.sortOrder()
            it.setImageResource(if (!aToZ) R.drawable.ic_expand_less_white_36dp else R.drawable.ic_expand_more_white_36dp)
        }
    }

    fun setReadFilter() {
        show_unread?.let {
            it.isChecked = presenter.onlyUnread()
        }
    }

    fun setDownloadedFilter() {
        show_downloaded?.let {
            it.isChecked = presenter.onlyDownloaded()
        }
    }

}
