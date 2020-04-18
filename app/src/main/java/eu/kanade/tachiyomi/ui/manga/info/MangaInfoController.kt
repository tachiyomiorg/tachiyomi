package eu.kanade.tachiyomi.ui.manga.info

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.chip.Chip
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.MangaInfoControllerBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.recent.history.HistoryController
import eu.kanade.tachiyomi.ui.recent.updates.UpdatesController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.global_search.GlobalSearchController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.lang.truncateCenter
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.toggle
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.view.longClicks
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Fragment that shows manga information.
 * Uses R.layout.manga_info_controller.
 * UI related actions should be called from here.
 */
class MangaInfoController(private val fromSource: Boolean = false) :
    NucleusController<MangaInfoControllerBinding, MangaInfoPresenter>(),
    ChangeMangaCategoriesDialog.Listener {

    private val preferences: PreferencesHelper by injectLazy()

    private var initialLoad: Boolean = true

    private var thumbnailUrl: String? = null

    override fun createPresenter(): MangaInfoPresenter {
        val ctrl = parentController as MangaController
        return MangaInfoPresenter(ctrl.manga!!, ctrl.source!!, ctrl.mangaFavoriteRelay)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = MangaInfoControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Set onclickListener to toggle favorite when favorite button clicked.
        binding.btnFavorite.clicks()
            .onEach { onFavoriteClick() }
            .launchIn(scope)

        // Set onLongClickListener to manage categories when favorite button is clicked.
        binding.btnFavorite.longClicks()
            .onEach { onFavoriteLongClick() }
            .launchIn(scope)

        if (presenter.source is HttpSource) {
            binding.btnWebview.visible()
            binding.btnShare.visible()

            binding.btnWebview.clicks()
                .onEach { openInWebView() }
                .launchIn(scope)
            binding.btnShare.clicks()
                .onEach { shareManga() }
                .launchIn(scope)
        }

        // Set SwipeRefresh to refresh manga data.
        binding.swipeRefresh.refreshes()
            .onEach { fetchMangaFromSource() }
            .launchIn(scope)

        binding.mangaFullTitle.longClicks()
            .onEach {
                copyToClipboard(view.context.getString(R.string.title), binding.mangaFullTitle.text.toString())
            }
            .launchIn(scope)

        binding.mangaFullTitle.clicks()
            .onEach {
                performGlobalSearch(binding.mangaFullTitle.text.toString())
            }
            .launchIn(scope)

        binding.mangaArtist.longClicks()
            .onEach {
                copyToClipboard(binding.mangaArtistLabel.text.toString(), binding.mangaArtist.text.toString())
            }
            .launchIn(scope)

        binding.mangaArtist.clicks()
            .onEach {
                performGlobalSearch(binding.mangaArtist.text.toString())
            }
            .launchIn(scope)

        binding.mangaAuthor.longClicks()
            .onEach {
                copyToClipboard(binding.mangaAuthor.text.toString(), binding.mangaAuthor.text.toString())
            }
            .launchIn(scope)

        binding.mangaAuthor.clicks()
            .onEach {
                performGlobalSearch(binding.mangaAuthor.text.toString())
            }
            .launchIn(scope)

        binding.mangaSummary.longClicks()
            .onEach {
                copyToClipboard(view.context.getString(R.string.description), binding.mangaSummary.text.toString())
            }
            .launchIn(scope)

        binding.mangaCover.longClicks()
            .onEach {
                copyToClipboard(view.context.getString(R.string.title), presenter.manga.title)
            }
            .launchIn(scope)
    }

    /**
     * Check if manga is initialized.
     * If true update view with manga information,
     * if false fetch manga information
     *
     * @param manga manga object containing information about manga.
     * @param source the source of the manga.
     */
    fun onNextManga(manga: Manga, source: Source) {
        if (manga.initialized) {
            // Update view.
            setMangaInfo(manga, source)
        } else {
            // Initialize manga.
            fetchMangaFromSource()
        }
    }

    /**
     * Update the view with manga information.
     *
     * @param manga manga object containing information about manga.
     * @param source the source of the manga.
     */
    private fun setMangaInfo(manga: Manga, source: Source?) {
        val view = view ?: return

        // update full title TextView.
        binding.mangaFullTitle.text = if (manga.title.isBlank()) {
            view.context.getString(R.string.unknown)
        } else {
            manga.title
        }

        // Update artist TextView.
        binding.mangaArtist.text = if (manga.artist.isNullOrBlank()) {
            view.context.getString(R.string.unknown)
        } else {
            manga.artist
        }

        // Update author TextView.
        binding.mangaAuthor.text = if (manga.author.isNullOrBlank()) {
            view.context.getString(R.string.unknown)
        } else {
            manga.author
        }

        // If manga source is known update source TextView.
        val mangaSource = source?.toString()
        with(binding.mangaSource) {
            if (mangaSource != null) {
                text = mangaSource
                setOnClickListener {
                    val sourceManager = Injekt.get<SourceManager>()
                    performSearch(sourceManager.getOrStub(source.id).name)
                }
            } else {
                text = view.context.getString(R.string.unknown)
            }
        }

        // Update status TextView.
        binding.mangaStatus.setText(when (manga.status) {
            SManga.ONGOING -> R.string.ongoing
            SManga.COMPLETED -> R.string.completed
            SManga.LICENSED -> R.string.licensed
            else -> R.string.unknown
        })

        // Set the favorite drawable to the correct one.
        setFavoriteButtonState(manga.favorite)

        // Set cover if it wasn't already.
        if (binding.mangaCover.drawable == null || manga.thumbnail_url != thumbnailUrl) {
            thumbnailUrl = manga.thumbnail_url
            val mangaThumbnail = manga.toMangaThumbnail()

            GlideApp.with(view.context)
                    .load(mangaThumbnail)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .into(binding.mangaCover)

            GlideApp.with(view.context)
                    .load(mangaThumbnail)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .into(binding.backdrop)
        }

        // Manga info section
        if (manga.description.isNullOrBlank() && manga.genre.isNullOrBlank()) {
            hideMangaInfo()
        } else {
            // Update description TextView.
            binding.mangaSummary.text = if (manga.description.isNullOrBlank()) {
                view.context.getString(R.string.unknown)
            } else {
                manga.description
            }

            // Update genres list
            if (!manga.genre.isNullOrBlank()) {
                binding.mangaGenresTags.removeAllViews()

                manga.getGenres()?.forEach { genre ->
                    val chip = Chip(view.context).apply {
                        text = genre
                        setOnClickListener { performSearch(genre) }
                    }

                    binding.mangaGenresTags.addView(chip)
                }
            }

            // Handle showing more or less info
            binding.mangaSummary.clicks()
                .onEach { toggleMangaInfo(view.context) }
                .launchIn(scope)
            binding.mangaInfoToggle.clicks()
                .onEach { toggleMangaInfo(view.context) }
                .launchIn(scope)

            // Expand manga info if navigated from source listing
            if (initialLoad && fromSource) {
                toggleMangaInfo(view.context)
                initialLoad = false
            }
        }
    }

    private fun hideMangaInfo() {
        binding.mangaSummaryLabel.gone()
        binding.mangaSummary.gone()
        binding.mangaGenresTags.gone()
        binding.mangaInfoToggle.gone()
    }

    private fun toggleMangaInfo(context: Context) {
        val isExpanded = binding.mangaInfoToggle.text == context.getString(R.string.manga_info_collapse)

        binding.mangaInfoToggle.text =
            if (isExpanded)
                context.getString(R.string.manga_info_expand)
            else
                context.getString(R.string.manga_info_collapse)

        with(binding.mangaSummary) {
            maxLines =
                if (isExpanded)
                    3
                else
                    Int.MAX_VALUE

            ellipsize =
                if (isExpanded)
                    TextUtils.TruncateAt.END
                else
                    null
        }

        binding.mangaGenresTags.toggle()
    }

    /**
     * Toggles the favorite status and asks for confirmation to delete downloaded chapters.
     */
    private fun toggleFavorite() {
        val view = view

        val isNowFavorite = presenter.toggleFavorite()
        if (view != null && !isNowFavorite && presenter.hasDownloads()) {
            view.snack(view.context.getString(R.string.delete_downloads_for_manga)) {
                setAction(R.string.action_delete) {
                    presenter.deleteDownloads()
                }
            }
        }
    }

    private fun openInWebView() {
        val source = presenter.source as? HttpSource ?: return

        val url = try {
            source.mangaDetailsRequest(presenter.manga).url.toString()
        } catch (e: Exception) {
            return
        }

        val activity = activity ?: return
        val intent = WebViewActivity.newIntent(activity, url, source.id, presenter.manga.title)
        startActivity(intent)
    }

    /**
     * Called to run Intent with [Intent.ACTION_SEND], which show share dialog.
     */
    private fun shareManga() {
        val context = view?.context ?: return

        val source = presenter.source as? HttpSource ?: return
        try {
            val url = source.mangaDetailsRequest(presenter.manga).url.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Update favorite button with correct drawable and text.
     *
     * @param isFavorite determines if manga is favorite or not.
     */
    private fun setFavoriteButtonState(isFavorite: Boolean) {
        // Set the Favorite drawable to the correct one.
        // Border drawable if false, filled drawable if true.
        binding.btnFavorite.apply {
            icon = ContextCompat.getDrawable(context, if (isFavorite) R.drawable.ic_bookmark_24dp else R.drawable.ic_add_to_library_24dp)
            text = context.getString(if (isFavorite) R.string.in_library else R.string.add_to_library)
            isChecked = isFavorite
        }
    }

    /**
     * Start fetching manga information from source.
     */
    private fun fetchMangaFromSource() {
        setRefreshing(true)
        // Call presenter and start fetching manga information
        presenter.fetchMangaFromSource()
    }

    /**
     * Update swipe refresh to stop showing refresh in progress spinner.
     */
    fun onFetchMangaDone() {
        setRefreshing(false)
    }

    /**
     * Update swipe refresh to start showing refresh in progress spinner.
     */
    fun onFetchMangaError(error: Throwable) {
        setRefreshing(false)
        activity?.toast(error.message)
    }

    /**
     * Set swipe refresh status.
     *
     * @param value whether it should be refreshing or not.
     */
    private fun setRefreshing(value: Boolean) {
        binding.swipeRefresh.isRefreshing = value
    }

    private fun onFavoriteClick() {
        val manga = presenter.manga

        if (manga.favorite) {
            toggleFavorite()
            activity?.toast(activity?.getString(R.string.manga_removed_library))
        } else {
            val categories = presenter.getCategories()
            val defaultCategoryId = preferences.defaultCategory()
            val defaultCategory = categories.find { it.id == defaultCategoryId }

            when {
                // Default category set
                defaultCategory != null -> {
                    toggleFavorite()
                    presenter.moveMangaToCategory(manga, defaultCategory)
                    activity?.toast(activity?.getString(R.string.manga_added_library))
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    toggleFavorite()
                    presenter.moveMangaToCategory(manga, null)
                    activity?.toast(activity?.getString(R.string.manga_added_library))
                }

                // Choose a category
                else -> {
                    val ids = presenter.getMangaCategoryIds(manga)
                    val preselected = ids.mapNotNull { id ->
                        categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                    }.toTypedArray()

                    ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                            .showDialog(router)
                }
            }
        }
    }

    private fun onFavoriteLongClick() {
        val manga = presenter.manga

        if (manga.favorite && presenter.getCategories().isNotEmpty()) {
            val categories = presenter.getCategories()

            val ids = presenter.getMangaCategoryIds(manga)
            val preselected = ids.mapNotNull { id ->
                categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
            }.toTypedArray()

            ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                    .showDialog(router)
        } else {
            onFavoriteClick()
        }
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        val manga = mangas.firstOrNull() ?: return

        if (!manga.favorite) {
            toggleFavorite()
        }

        presenter.moveMangaToCategories(manga, categories)
        activity?.toast(activity?.getString(R.string.manga_added_library))
    }

    /**
     * Copies a string to clipboard
     *
     * @param label Label to show to the user describing the content
     * @param content the actual text to copy to the board
     */
    private fun copyToClipboard(label: String, content: String) {
        if (content.isBlank()) return

        val activity = activity ?: return
        val view = view ?: return

        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))

        activity.toast(view.context.getString(R.string.copied_to_clipboard, content.truncateCenter(20)),
                Toast.LENGTH_SHORT)
    }

    /**
     * Perform a global search using the provided query.
     *
     * @param query the search query to pass to the search controller
     */
    private fun performGlobalSearch(query: String) {
        val router = parentController?.router ?: return
        router.pushController(GlobalSearchController(query).withFadeTransaction())
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    private fun performSearch(query: String) {
        val router = parentController?.router ?: return

        if (router.backstackSize < 2) {
            return
        }

        when (val previousController = router.backstack[router.backstackSize - 2].controller()) {
            is LibraryController -> {
                router.handleBack()
                previousController.search(query)
            }
            is UpdatesController,
            is HistoryController -> {
                // Manually navigate to LibraryController
                router.handleBack()
                (router.activity as MainActivity).setSelectedNavItem(R.id.nav_library)
                val controller = router.getControllerWithTag(R.id.nav_library.toString()) as LibraryController
                controller.search(query)
            }
            is BrowseSourceController -> {
                router.handleBack()
                previousController.searchWithQuery(query)
            }
        }
    }
}
