package eu.kanade.tachiyomi.ui.manga.info;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.model.LazyHeaders;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.cache.CoverCache;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment;
import nucleus.factory.RequiresPresenter;

/**
 * Fragment that shows manga information.
 * Uses R.layout.fragment_manga_info.
 * UI related actions should be called from here.
 */
@RequiresPresenter(MangaInfoPresenter.class)
public class MangaInfoFragment extends BaseRxFragment<MangaInfoPresenter> {
    /**
     * SwipeRefreshLayout showing refresh status
     */
    @Bind(R.id.swipe_refresh) SwipeRefreshLayout swipeRefresh;

    /**
     * TextView containing artist information.
     */
    @Bind(R.id.manga_artist) TextView artist;

    /**
     * TextView containing author information.
     */
    @Bind(R.id.manga_author) TextView author;

    /**
     * TextView containing chapter count.
     */
    @Bind(R.id.manga_chapters) TextView chapterCount;

    /**
     * TextView containing genres.
     */
    @Bind(R.id.manga_genres) TextView genres;

    /**
     * TextView containing status (ongoing, finished).
     */
    @Bind(R.id.manga_status) TextView status;

    /**
     * TextView containing source.
     */
    @Bind(R.id.manga_source) TextView source;

    /**
     * TextView containing manga summary.
     */
    @Bind(R.id.manga_summary) TextView description;

    /**
     * ImageView of cover.
     */
    @Bind(R.id.manga_cover) ImageView cover;

    /**
     * ImageView containing manga cover shown as blurred backdrop.
     */
    @Bind(R.id.backdrop) ImageView backdrop;

    /**
     * FAB anchored to bottom of top view used to (add / remove) manga (to / from) library.
     */
    @Bind(R.id.fab_favorite) FloatingActionButton fabFavorite;

    /**
     * Create new instance of MangaInfoFragment.
     *
     * @return MangaInfoFragment.
     */
    public static MangaInfoFragment newInstance() {
        return new MangaInfoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment.
        View view = inflater.inflate(R.layout.fragment_manga_info, container, false);

        // Bind layout objects.
        ButterKnife.bind(this, view);

        // Set onclickListener to toggle favorite when FAB clicked.
        fabFavorite.setOnClickListener(v -> getPresenter().toggleFavorite());

        // Set SwipeRefresh to refresh manga data.
        swipeRefresh.setOnRefreshListener(this::fetchMangaFromSource);

        return view;
    }

    /**
     * Check if manga is initialized.
     * If true update view with manga information,
     * if false fetch manga information
     *
     * @param manga  manga object containing information about manga.
     * @param source the source of the manga.
     */
    public void onNextManga(Manga manga, Source source) {
        if (manga.initialized) {
            // Update view.
            setMangaInfo(manga, source);
        } else {
            // Initialize manga.
            fetchMangaFromSource();
        }
    }

    /**
     * Update the view with manga information.
     *
     * @param manga       manga object containing information about manga.
     * @param mangaSource the source of the manga.
     */
    private void setMangaInfo(Manga manga, Source mangaSource) {
        // Update artist TextView.
        artist.setText(manga.artist);

        // Update author TextView.
        author.setText(manga.author);

        // If manga source is known update source TextView.
        if (mangaSource != null) {
            source.setText(mangaSource.getVisibleName());
        }

        // Update genres TextView.
        genres.setText(manga.genre);

        // Update status TextView.
        status.setText(manga.getStatus(getActivity()));

        // Update description TextView.
        description.setText(manga.description);

        // Set the favorite drawable to the correct one.
        setFavoriteDrawable(manga.favorite);

        // Initialize CoverCache and Glide headers to retrieve cover information.
        CoverCache coverCache = getPresenter().coverCache;
        LazyHeaders headers = getPresenter().source.getGlideHeaders();

        // Check if thumbnail_url is given.
        if (manga.thumbnail_url != null) {
            // Check if cover is already drawn.
            if (cover.getDrawable() == null) {
                // If manga is in library then (download / save) (from / to) local cache if available,
                // else download from network.
                if (manga.favorite) {
                    coverCache.saveOrLoadFromCache(cover, manga.thumbnail_url, headers);
                } else {
                    coverCache.loadFromNetwork(cover, manga.thumbnail_url, headers);
                }
            }
            // Check if backdrop is already drawn.
            if (backdrop.getDrawable() == null) {
                // If manga is in library then (download / save) (from / to) local cache if available,
                // else download from network.
                if (manga.favorite) {
                    coverCache.saveOrLoadFromCache(backdrop, manga.thumbnail_url, headers);
                } else {
                    coverCache.loadFromNetwork(backdrop, manga.thumbnail_url, headers);
                }
            }
        }
    }

    /**
     * Update chapter count TextView.
     *
     * @param count number of chapters.
     */
    public void setChapterCount(int count) {
        chapterCount.setText(String.valueOf(count));
    }

    /**
     * Update FAB with correct drawable.
     *
     * @param isFavorite determines if manga is favorite or not.
     */
    private void setFavoriteDrawable(boolean isFavorite) {
        // Set the Favorite drawable to the correct one.
        // Border drawable if false, filled drawable if true.
        fabFavorite.setImageDrawable(ContextCompat.getDrawable(getContext(), isFavorite ?
                R.drawable.ic_bookmark_white_24dp :
                R.drawable.ic_bookmark_border_white_24dp));
    }

    /**
     * Start fetching manga information from source.
     */
    private void fetchMangaFromSource() {
        setRefreshing(true);
        // Call presenter and start fetching manga information
        getPresenter().fetchMangaFromSource();
    }


    /**
     * Update swipeRefresh to stop showing refresh in progress spinner.
     */
    public void onFetchMangaDone() {
        setRefreshing(false);
    }

    /**
     * Update swipeRefresh to start showing refresh in progress spinner.
     */
    public void onFetchMangaError() {
        setRefreshing(false);
    }

    /**
     * Set swipeRefresh status.
     *
     * @param value status of manga fetch.
     */
    private void setRefreshing(boolean value) {
        swipeRefresh.setRefreshing(value);
    }
}
