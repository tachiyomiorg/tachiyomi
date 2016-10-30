package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.widget.SimpleTextWatcher
import kotlinx.android.synthetic.main.dialog_track_search.view.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class TrackSearchDialog : DialogFragment() {

    companion object {

        fun newInstance(): TrackSearchDialog {
            return TrackSearchDialog()
        }
    }

    private lateinit var v: View

    lateinit var adapter: TrackSearchAdapter
        private set

    private val queryRelay by lazy { PublishRelay.create<String>() }

    private var searchDebounceSubscription: Subscription? = null

    private var selectedItem: MangaSync? = null

    val presenter: TrackPresenter
        get() = (parentFragment as TrackFragment).presenter

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(context)
                .customView(R.layout.dialog_track_search, false)
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
        adapter = TrackSearchAdapter(context)
        view.track_search_list.adapter = adapter

        // Set listeners
        selectedItem = null
        view.track_search_list.setOnItemClickListener { parent, viewList, position, id ->
            selectedItem = adapter.getItem(position)
        }

        // Do an initial search based on the manga's title
        if (savedState == null) {
            val title = presenter.manga.title
            view.track_search.append(title)
            search(title)
        }

        view.track_search.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                queryRelay.call(s.toString())
            }
        })
    }

    override fun onResume() {
        super.onResume()

        // Listen to text changes
        searchDebounceSubscription = queryRelay.debounce(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it.isNotBlank() }
                .subscribe { search(it) }
    }

    override fun onPause() {
        searchDebounceSubscription?.unsubscribe()
        super.onPause()
    }

    private fun search(query: String) {
        v.progress.visibility = View.VISIBLE
        v.track_search_list.visibility = View.GONE

        presenter.search(query)
    }

    fun onSearchResults(results: List<MangaSync>) {
        selectedItem = null
        v.progress.visibility = View.GONE
        v.track_search_list.visibility = View.VISIBLE
        adapter.setItems(results)
    }

    fun onSearchResultsError() {
        v.progress.visibility = View.VISIBLE
        v.track_search_list.visibility = View.GONE
        adapter.setItems(emptyList())
    }

    private fun onPositiveButtonClick() {
        presenter.registerTracking(selectedItem)
    }

}