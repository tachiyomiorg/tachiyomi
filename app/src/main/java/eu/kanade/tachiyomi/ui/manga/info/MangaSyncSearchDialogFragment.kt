package eu.kanade.tachiyomi.ui.manga.info

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.widget.SimpleTextWatcher
import kotlinx.android.synthetic.main.dialog_manga_sync_search.view.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Fragment that shows search information and status.
 * Uses [R.layout.dialog_manga_sync_search].
 * UI related actions should be called from here.
 */
class MangaSyncSearchDialogFragment : DialogFragment() {

    companion object {

        /**
         * Create new [MangaSyncSearchDialogFragment].
         */
        fun newInstance(syncId: Int): MangaSyncSearchDialogFragment {
            val fragment = MangaSyncSearchDialogFragment()
            fragment.syncId = syncId
            return fragment
        }
    }

    /**
     * Custom view of Dialog
     */
    lateinit var mView: View
        private set

    /**
     * Instance of [MangaSyncSearchDialogAdapter]
     */
    lateinit var adapter: MangaSyncSearchDialogAdapter
        private set

    /**
     * String containing the search query
     */
    lateinit var querySubject: PublishSubject<String>
        private set

    /**
     * Selected [MangaSync] from [MangaSyncSearchDialogAdapter]
     */
    private var selectedItem: MangaSync? = null

    /**
     * Subscription which is subscribed when searching [MangaSync]
     */
    private var searchSubscription: Subscription? = null

    /**
     * Id of service
     */
    private var syncId = 0

    /**
     * Presenter that handles Observable calls.
     */
    val presenter: MangaInfoPresenter
        get() = fragment.presenter

    /**
     * Fragment that shows manga information and sync status.
     */
    val fragment: MangaInfoFragment
        get() = parentFragment as MangaInfoFragment

    /**
     * This method will be called after onCreate(Bundle)
     * @param savedInstanceState The last saved instance state of the Fragment.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity)
                .customView(R.layout.dialog_manga_sync_search, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { dialog1, which -> onPositiveButtonClick() }
                .build()

        onViewCreated(dialog.view, savedInstanceState)

        return dialog
    }

    /**
     * Called immediately after onCreateView()
     * @param view The View returned by onCreateDialog.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mView = view

        // Create adapter
        adapter = MangaSyncSearchDialogAdapter(activity)
        view.search_results.adapter = adapter

        // Set listeners
        view.search_results.setOnItemClickListener { parent, viewList, position, id ->
            selectedItem = adapter.getItem(position)
        }

        // Do an initial search based on the manga's title
        if (savedInstanceState == null) {
            val title = presenter.manga.title
            view.search_field.append(title)
            search(title, syncId)
        }

        // Initialize query subject
        querySubject = PublishSubject.create<String>()

        // Set on text change listener
        view.search_field.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                querySubject.onNext(s.toString())
            }
        })
    }

    override fun onResume() {
        super.onResume()

        // Listen to text changes
        searchSubscription = querySubject.debounce(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { search(it, syncId) }
    }

    override fun onPause() {
        searchSubscription?.unsubscribe()
        super.onPause()
    }

    private fun onPositiveButtonClick() {
        presenter.registerManga(selectedItem!!)
    }

    fun search(query: String, syncId: Int, force: Boolean = false) {
        if (!query.isNullOrEmpty()) {
            this.syncId = syncId
            mView.search_results.visibility = View.GONE
            mView.progress.visibility = View.VISIBLE
            presenter.searchManga(query, syncId, force)
        }
    }

    fun onSearchResults(results: List<MangaSync>) {
        selectedItem = null
        mView.progress.visibility = View.GONE
        mView.search_results.visibility = View.VISIBLE
        adapter.setItems(results)
    }

    fun onSearchResultsError() {
        mView.progress.visibility = View.GONE
        mView.search_results.visibility = View.VISIBLE
        adapter.clear()
    }
}
