package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackSearchDialogBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.view.setNavigationBarTransparentCompat
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.editorActionEvents
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackSearchDialog : DialogController {

    private var binding: TrackSearchDialogBinding? = null

    private var adapter: TrackSearchAdapter? = null

    private val service: TrackService
    private val currentTrackUrl: String?

    private val trackController
        get() = targetController as MangaController

    private lateinit var currentlySearched: String

    constructor(
        target: MangaController,
        _service: TrackService,
        _currentTrackUrl: String?
    ) : super(bundleOf(KEY_SERVICE to _service.id, KEY_CURRENT_URL to _currentTrackUrl)) {
        targetController = target
        service = _service
        currentTrackUrl = _currentTrackUrl
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        service = Injekt.get<TrackManager>().getService(bundle.getInt(KEY_SERVICE))!!
        currentTrackUrl = bundle.getString(KEY_CURRENT_URL)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = TrackSearchDialogBinding.inflate(LayoutInflater.from(activity!!))

        // Toolbar stuff
        binding!!.toolbar.setNavigationOnClickListener { dialog?.dismiss() }
        binding!!.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.done -> {
                    val adapter = adapter ?: return@setOnMenuItemClickListener true
                    val item = adapter.items.getOrNull(adapter.selectedItemPosition)
                    if (item != null) {
                        trackController.presenter.registerTracking(item, service)
                        dialog?.dismiss()
                    }
                }
                R.id.remove -> {
                    trackController.presenter.unregisterTracking(service)
                    dialog?.dismiss()
                }
            }
            true
        }
        binding!!.toolbar.menu.findItem(R.id.remove).isVisible = currentTrackUrl != null

        // Create adapter
        adapter = TrackSearchAdapter(currentTrackUrl) { which ->
            binding!!.toolbar.menu.findItem(R.id.done).isEnabled = which != null
        }
        binding!!.trackSearchRecyclerview.adapter = adapter

        // Do an initial search based on the manga's title
        if (savedViewState == null) {
            currentlySearched = trackController.presenter.manga.title
            binding!!.titleInput.editText?.append(currentlySearched)
        }
        search(currentlySearched)

        // Input listener
        binding?.titleInput?.editText
            ?.editorActionEvents {
                when (it.actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        true
                    }
                    else -> {
                        it.keyEvent?.action == KeyEvent.ACTION_DOWN && it.keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER
                    }
                }
            }
            ?.filter { it.view.text.isNotBlank() }
            ?.onEach {
                val query = it.view.text.toString()
                if (query != currentlySearched) {
                    currentlySearched = query
                    search(it.view.text.toString())
                    it.view.context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(it.view.windowToken, 0)
                    it.view.clearFocus()
                }
            }
            ?.launchIn(trackController.viewScope)

        // Edge to edge
        binding!!.appbar.applyInsetter {
            type(navigationBars = true, statusBars = true) {
                padding(left = true, top = true, right = true)
            }
        }
        binding!!.titleInput.applyInsetter {
            type(navigationBars = true) {
                margin(horizontal = true)
            }
        }
        binding!!.progress.applyInsetter {
            type(navigationBars = true) {
                margin()
            }
        }
        binding!!.message.applyInsetter {
            type(navigationBars = true) {
                margin()
            }
        }
        binding!!.trackSearchRecyclerview.applyInsetter {
            type(navigationBars = true) {
                padding(vertical = true)
                margin(horizontal = true)
            }
        }

        return AppCompatDialog(activity!!, R.style.ThemeOverlay_Tachiyomi_Dialog_Fullscreen).apply {
            setContentView(binding!!.root)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        dialog?.window?.let { window ->
            window.setNavigationBarTransparentCompat(window.context)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        binding = null
        adapter = null
    }

    private fun search(query: String) {
        val binding = binding ?: return
        binding.progress.isVisible = true
        binding.trackSearchRecyclerview.isVisible = false
        binding.message.isVisible = false
        trackController.presenter.trackingSearch(query, service)
    }

    fun onSearchResults(results: List<TrackSearch>) {
        val binding = binding ?: return
        binding.progress.isVisible = false

        val emptyResult = results.isEmpty()
        adapter?.items = results
        binding.trackSearchRecyclerview.isVisible = !emptyResult
        binding.trackSearchRecyclerview.scrollToPosition(0)
        binding.message.isVisible = emptyResult
        if (emptyResult) {
            binding.message.text = binding.message.context.getString(R.string.no_results_found)
        }
    }

    fun onSearchResultsError(message: String?) {
        val binding = binding ?: return
        binding.progress.isVisible = false
        binding.trackSearchRecyclerview.isVisible = false
        binding.message.isVisible = true
        binding.message.text = message ?: binding.message.context.getString(R.string.unknown_error)
        adapter?.items = emptyList()
    }

    private companion object {
        const val KEY_SERVICE = "service_id"
        const val KEY_CURRENT_URL = "current_url"
    }
}
