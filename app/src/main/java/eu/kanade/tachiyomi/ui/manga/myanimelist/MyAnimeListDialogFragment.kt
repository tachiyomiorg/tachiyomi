package eu.kanade.tachiyomi.ui.manga.myanimelist

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.widget.SimpleTextWatcher
import kotlinx.android.synthetic.main.dialog_myanimelist_search.view.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MyAnimeListDialogFragment : DialogFragment() {

    companion object {

        fun newInstance(): MyAnimeListDialogFragment {
            return MyAnimeListDialogFragment()
        }
    }

    private lateinit var v: View

    lateinit var adapter: MyAnimeListSearchAdapter
        private set

    lateinit var querySubject: PublishSubject<String>
        private set

    private var selectedItem: MangaSync? = null

    private var searchSubscription: Subscription? = null

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity)
                .customView(R.layout.dialog_myanimelist_search, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { dialog1, which -> onPositiveButtonClick() }
                .build()

        onViewCreated(dialog.view, savedState)

        return dialog
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        v = view

        // Create adapter
        adapter = MyAnimeListSearchAdapter(activity)
        view.myanimelist_search_results.adapter = adapter

        // Set listeners
        view.myanimelist_search_results.setOnItemClickListener { parent, viewList, position, id ->
            selectedItem = adapter.getItem(position)
        }

        // Do an initial search based on the manga's title
        if (savedState == null) {
            val title = presenter.manga.title
            view.myanimelist_search_field.append(title)
            search(title)
        }

        querySubject = PublishSubject.create<String>()

        view.myanimelist_search_field.addTextChangedListener(object : SimpleTextWatcher() {
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
                .subscribe { search(it) }
    }

    override fun onPause() {
        searchSubscription?.unsubscribe()
        super.onPause()
    }

    private fun onPositiveButtonClick() {
        selectedItem?.let {
            presenter.registerManga(it)
        }
    }

    private fun search(query: String) {
        if (!query.isNullOrEmpty()) {
            v.myanimelist_search_results.visibility = View.GONE
            v.progress.visibility = View.VISIBLE
            presenter.searchManga(query)
        }
    }

    fun onSearchResults(results: List<MangaSync>) {
        selectedItem = null
        v.progress.visibility = View.GONE
        v.myanimelist_search_results.visibility = View.VISIBLE
        adapter.setItems(results)
    }

    fun onSearchResultsError() {
        v.progress.visibility = View.GONE
        v.myanimelist_search_results.visibility = View.VISIBLE
        adapter.clear()
    }

    val malFragment: MyAnimeListFragment
        get() = parentFragment as MyAnimeListFragment

    val presenter: MyAnimeListPresenter
        get() = malFragment.presenter

}
