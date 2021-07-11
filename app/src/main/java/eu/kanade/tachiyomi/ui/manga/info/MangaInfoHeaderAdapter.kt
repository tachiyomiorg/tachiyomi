package eu.kanade.tachiyomi.ui.manga.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.loadAny
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.MangaInfoHeaderBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.view.setChips
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.view.longClicks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaInfoHeaderAdapter(
    private val controller: MangaController,
    private val fromSource: Boolean,
    private val isTablet: Boolean,
) :
    RecyclerView.Adapter<MangaInfoHeaderAdapter.HeaderViewHolder>() {

    private val trackManager: TrackManager by injectLazy()

    private var manga: Manga = controller.presenter.manga
    private var source: Source = controller.presenter.source
    private var trackCount: Int = 0

    private lateinit var binding: MangaInfoHeaderBinding

    private var initialLoad: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = MangaInfoHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding.root)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    /**
     * Update the view with manga information.
     *
     * @param manga manga object containing information about manga.
     * @param source the source of the manga.
     */
    fun update(manga: Manga, source: Source) {
        this.manga = manga
        this.source = source

        notifyDataSetChanged()
    }

    fun setTrackingCount(trackCount: Int) {
        this.trackCount = trackCount

        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            // For rounded corners
            binding.mangaCover.clipToOutline = true

            binding.btnFavorite.clicks()
                .onEach { controller.onFavoriteClick() }
                .launchIn(controller.viewScope)

            if (controller.presenter.manga.favorite && controller.presenter.getCategories().isNotEmpty()) {
                binding.btnFavorite.longClicks()
                    .onEach { controller.onCategoriesClick() }
                    .launchIn(controller.viewScope)
            }

            with(binding.btnTracking) {
                if (trackManager.hasLoggedServices()) {
                    isVisible = true

                    if (trackCount > 0) {
                        setIconResource(R.drawable.ic_done_24dp)
                        text = view.context.resources.getQuantityString(
                            R.plurals.num_trackers,
                            trackCount,
                            trackCount
                        )
                        isActivated = true
                    } else {
                        setIconResource(R.drawable.ic_sync_24dp)
                        text = view.context.getString(R.string.manga_tracking_tab)
                        isActivated = false
                    }

                    clicks()
                        .onEach { controller.onTrackingClick() }
                        .launchIn(controller.viewScope)
                } else {
                    isVisible = false
                }
            }

            if (controller.presenter.source is HttpSource) {
                binding.btnWebview.isVisible = true
                binding.btnWebview.clicks()
                    .onEach { controller.openMangaInWebView() }
                    .launchIn(controller.viewScope)
            }

            binding.mangaFullTitle.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        binding.mangaFullTitle.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaFullTitle.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaFullTitle.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaAuthor.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaAuthor.text.toString(),
                        binding.mangaAuthor.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaAuthor.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaAuthor.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaArtist.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaArtist.text.toString(),
                        binding.mangaArtist.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaArtist.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaArtist.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaSummaryText.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.description),
                        binding.mangaSummaryText.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaCover.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        controller.presenter.manga.title
                    )
                }
                .launchIn(controller.viewScope)

            setMangaInfo(manga, source)
        }

        /**
         * Update the view with manga information.
         *
         * @param manga manga object containing information about manga.
         * @param source the source of the manga.
         */
        private fun setMangaInfo(manga: Manga, source: Source?) {
            // Update full title TextView.
            binding.mangaFullTitle.text = if (manga.title.isBlank()) {
                view.context.getString(R.string.unknown)
            } else {
                manga.title
            }

            // Update author TextView.
            binding.mangaAuthor.text = if (manga.author.isNullOrBlank()) {
                view.context.getString(R.string.unknown_author)
            } else {
                manga.author
            }

            // Update artist TextView.
            val hasArtist = !manga.artist.isNullOrBlank() && manga.artist != manga.author
            binding.mangaArtist.isVisible = hasArtist
            if (hasArtist) {
                binding.mangaArtist.text = manga.artist
            }

            // If manga source is known update source TextView.
            val mangaSource = source?.toString()
            with(binding.mangaSource) {
                if (mangaSource != null) {
                    text = mangaSource
                    setOnClickListener {
                        val sourceManager = Injekt.get<SourceManager>()
                        controller.performSearch(sourceManager.getOrStub(source.id).name)
                    }
                } else {
                    text = view.context.getString(R.string.unknown)
                }
            }

            // Update status TextView.
            binding.mangaStatus.setText(
                when (manga.status) {
                    SManga.ONGOING -> R.string.ongoing
                    SManga.COMPLETED -> R.string.completed
                    SManga.LICENSED -> R.string.licensed
                    else -> R.string.unknown_status
                }
            )

            // Set the favorite drawable to the correct one.
            setFavoriteButtonState(manga.favorite)

            // Set cover if changed.
            listOfNotNull(binding.mangaCover, binding.backdrop).forEach {
                it.loadAny(manga)
            }

            // Manga info section
            val hasInfoContent = !manga.description.isNullOrBlank() || !manga.genre.isNullOrBlank()
            showMangaInfo(hasInfoContent)
            if (hasInfoContent) {
                // Update description TextView.
                binding.mangaSummaryText.text = if (manga.description.isNullOrBlank()) {
                    view.context.getString(R.string.unknown)
                } else {
                    manga.description
                }

                // Update genres list
                if (!manga.genre.isNullOrBlank()) {
                    binding.mangaGenresTagsCompactChips.setChips(
                        manga.getGenres(),
                        controller::performSearch
                    )
                    binding.mangaGenresTagsFullChips.setChips(
                        manga.getGenres(),
                        controller::performSearch
                    )
                } else {
                    binding.mangaGenresTagsCompactChips.isVisible = false
                    binding.mangaGenresTagsFullChips.isVisible = false
                }

                // Handle showing more or less info
                merge(
                    binding.mangaSummaryText.clicks(),
                    binding.mangaInfoToggleMore.clicks(),
                    binding.mangaInfoToggleLess.clicks(),
                    binding.mangaSummarySection.clicks()
                )
                    .onEach { toggleMangaInfo() }
                    .launchIn(controller.viewScope)

                // Expand manga info if navigated from source listing or explicitly set to
                // (e.g. on tablets)
                if (initialLoad && (fromSource || isTablet)) {
                    toggleMangaInfo()
                    initialLoad = false
                    // wrap_content and autoFixTextSize can cause unwanted behaviour this tries to solve it
                    binding.mangaFullTitle.requestLayout()
                }

                // Refreshes will change the state and it needs to be set to correct state to display correctly
                if (binding.mangaSummaryText.maxLines == 2) {
                    binding.mangaSummarySection.transitionToState(R.id.start)
                } else {
                    binding.mangaSummarySection.transitionToState(R.id.end)
                }
            }
        }

        private fun showMangaInfo(visible: Boolean) {
            binding.mangaSummarySection.isVisible = visible
        }

        private fun toggleMangaInfo() {
            val isCurrentlyExpanded = binding.mangaSummaryText.maxLines != 2

            if (isCurrentlyExpanded) {
                binding.mangaSummarySection.transitionToStart()
            } else {
                binding.mangaSummarySection.transitionToEnd()
            }

            binding.mangaSummaryText.maxLines = if (isCurrentlyExpanded) {
                2
            } else {
                Int.MAX_VALUE
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
                setIconResource(if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_border_24dp)
                text =
                    context.getString(if (isFavorite) R.string.in_library else R.string.add_to_library)
                isActivated = isFavorite
            }
        }
    }
}
