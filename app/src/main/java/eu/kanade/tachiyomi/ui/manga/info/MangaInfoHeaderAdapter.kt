package eu.kanade.tachiyomi.ui.manga.info

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.MangaThumbnail
import eu.kanade.tachiyomi.data.glide.toMangaThumbnail
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.MangaInfoHeaderBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.setChips
import eu.kanade.tachiyomi.util.view.setTooltip
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.view.longClicks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaInfoHeaderAdapter(
    private val controller: MangaController,
    private val fromSource: Boolean
) :
    RecyclerView.Adapter<MangaInfoHeaderAdapter.HeaderViewHolder>() {

    private var manga: Manga = controller.presenter.manga
    private var source: Source = controller.presenter.source

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private lateinit var binding: MangaInfoHeaderBinding

    private var initialLoad: Boolean = true
    private var currentMangaThumbnail: MangaThumbnail? = null

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

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            // For rounded corners
            binding.mangaCover.clipToOutline = true

            binding.btnFavorite.clicks()
                .onEach { controller.onFavoriteClick() }
                .launchIn(scope)

            if (controller.presenter.manga.favorite && Injekt.get<TrackManager>().hasLoggedServices()) {
                binding.btnTracking.visible()
                binding.btnTracking.clicks()
                    .onEach { controller.onTrackingClick() }
                    .launchIn(scope)
            } else {
                binding.btnTracking.gone()
            }

            if (controller.presenter.manga.favorite && controller.presenter.getCategories().isNotEmpty()) {
                binding.btnCategories.visible()
                binding.btnCategories.clicks()
                    .onEach { controller.onCategoriesClick() }
                    .launchIn(scope)
                binding.btnCategories.setTooltip(R.string.action_move_category)
            } else {
                binding.btnCategories.gone()
            }

            if (controller.presenter.source is HttpSource) {
                binding.btnWebview.visible()
                binding.btnWebview.clicks()
                    .onEach { controller.openMangaInWebView() }
                    .launchIn(scope)
                binding.btnWebview.setTooltip(R.string.action_open_in_web_view)

                binding.btnShare.visible()
                binding.btnShare.clicks()
                    .onEach { controller.shareManga() }
                    .launchIn(scope)
                binding.btnShare.setTooltip(R.string.action_share)
            }

            binding.mangaFullTitle.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        binding.mangaFullTitle.text.toString()
                    )
                }
                .launchIn(scope)

            binding.mangaFullTitle.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaFullTitle.text.toString())
                }
                .launchIn(scope)

            binding.mangaAuthor.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaAuthor.text.toString(),
                        binding.mangaAuthor.text.toString()
                    )
                }
                .launchIn(scope)

            binding.mangaAuthor.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaAuthor.text.toString())
                }
                .launchIn(scope)

            binding.mangaArtist.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaArtist.text.toString(),
                        binding.mangaArtist.text.toString()
                    )
                }
                .launchIn(scope)

            binding.mangaArtist.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaArtist.text.toString())
                }
                .launchIn(scope)

            binding.mangaSummary.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.description),
                        binding.mangaSummary.text.toString()
                    )
                }
                .launchIn(scope)

            binding.mangaCover.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        controller.presenter.manga.title
                    )
                }
                .launchIn(scope)

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
            val mangaThumbnail = manga.toMangaThumbnail()
            if (mangaThumbnail != currentMangaThumbnail) {
                currentMangaThumbnail = mangaThumbnail
                listOf(binding.mangaCover, binding.backdrop)
                    .forEach {
                        GlideApp.with(view.context)
                            .load(mangaThumbnail)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .centerCrop()
                            .into(it)
                    }
            }

            // Manga info section
            val hasInfoContent = !manga.description.isNullOrBlank() || !manga.genre.isNullOrBlank()
            showMangaInfo(hasInfoContent)
            if (hasInfoContent) {
                // Update description TextView.
                binding.mangaSummary.text = if (manga.description.isNullOrBlank()) {
                    view.context.getString(R.string.unknown)
                } else {
                    manga.description
                }

                // Update genres list
                if (!manga.genre.isNullOrBlank()) {
                    binding.mangaGenresTagsCompactChips.setChips(manga.getGenres(), controller::performSearch)
                    binding.mangaGenresTagsFullChips.setChips(manga.getGenres(), controller::performSearch)
                } else {
                    binding.mangaGenresTagsWrapper.gone()
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

            binding.btnCategories.visibleIf { manga.favorite && controller.presenter.getCategories().isNotEmpty() }
        }

        private fun showMangaInfo(visible: Boolean) {
            binding.mangaSummaryLabel.visibleIf { visible }
            binding.mangaSummary.visibleIf { visible }
            binding.mangaGenresTagsWrapper.visibleIf { visible }
            binding.mangaInfoToggle.visibleIf { visible }
        }

        private fun toggleMangaInfo(context: Context) {
            val isExpanded =
                binding.mangaInfoToggle.text == context.getString(R.string.manga_info_collapse)

            with(binding.mangaInfoToggle) {
                text = if (isExpanded) {
                    context.getString(R.string.manga_info_expand)
                } else {
                    context.getString(R.string.manga_info_collapse)
                }

                icon = if (isExpanded) {
                    context.getDrawable(R.drawable.ic_baseline_expand_more_24dp)
                } else {
                    context.getDrawable(R.drawable.ic_baseline_expand_less_24dp)
                }
            }

            with(binding.mangaSummary) {
                maxLines =
                    if (isExpanded) {
                        2
                    } else {
                        Int.MAX_VALUE
                    }

                ellipsize =
                    if (isExpanded) {
                        TextUtils.TruncateAt.END
                    } else {
                        null
                    }
            }

            binding.mangaGenresTagsCompact.visibleIf { isExpanded }
            binding.mangaGenresTagsFullChips.visibleIf { !isExpanded }
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
                icon = ContextCompat.getDrawable(
                    context,
                    if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_border_24dp
                )
                text =
                    context.getString(if (isFavorite) R.string.in_library else R.string.add_to_library)
                isChecked = isFavorite
            }
        }
    }
}
