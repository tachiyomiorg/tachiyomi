package eu.kanade.tachiyomi.ui.manga.info;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

@RequiresPresenter(MangaInfoPresenter.class)
public class MangaInfoFragment extends BaseRxFragment<MangaInfoPresenter> {

    @Bind(R.id.swipe_refresh) SwipeRefreshLayout swipeRefresh;

    @Bind(R.id.manga_artist) TextView artist;
    @Bind(R.id.manga_author) TextView author;
    @Bind(R.id.manga_chapters) TextView chapterCount;
    @Bind(R.id.manga_genres) TextView genres;
    @Bind(R.id.manga_status) TextView status;
    @Bind(R.id.manga_source) TextView source;
    @Bind(R.id.manga_summary) TextView description;
    @Bind(R.id.manga_cover) ImageView cover;

    @Bind(R.id.action_favorite) Button favoriteBtn;


    public static MangaInfoFragment newInstance() {
        return new MangaInfoFragment();
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_manga_info, container, false);
        ButterKnife.bind(this, view);

        favoriteBtn.setOnClickListener(v -> {
            getPresenter().toggleFavorite();
        });
        swipeRefresh.setOnRefreshListener(this::fetchMangaFromSource);

        return view;
    }

    public void onNextManga(Manga manga, Source source) {
        if (manga.initialized) {
            setMangaInfo(manga, source);
        } else {
            // Initialize manga
            fetchMangaFromSource();
        }
    }

    private void setMangaInfo(Manga manga, Source mangaSource) {
        artist.setText(manga.artist);
        author.setText(manga.author);

        if (mangaSource != null) {
            source.setText(mangaSource.getName());
        }
        genres.setText(manga.genre);
        status.setText(manga.getStatus(getActivity()));
        description.setText(manga.description);

        setFavoriteText(manga.favorite);

        CoverCache coverCache = getPresenter().coverCache;
        LazyHeaders headers = getPresenter().source.getGlideHeaders();
        if (manga.thumbnail_url != null && cover.getDrawable() == null) {
            if (manga.favorite) {
                coverCache.saveAndLoadFromCache(cover, manga.thumbnail_url, headers);
            } else {
                coverCache.loadFromNetwork(cover, manga.thumbnail_url, headers);
            }
        }
    }

    public void setChapterCount(int count) {
        chapterCount.setText(String.valueOf(count));
    }

    public void setFavoriteText(boolean isFavorite) {
        favoriteBtn.setText(!isFavorite ? R.string.add_to_library : R.string.remove_from_library);
    }

    private void fetchMangaFromSource() {
        setRefreshing(true);
        getPresenter().fetchMangaFromSource();
    }

    public void onFetchMangaDone() {
        setRefreshing(false);
    }

    public void onFetchMangaError() {
        setRefreshing(false);
    }

    private void setRefreshing(boolean value) {
        swipeRefresh.setRefreshing(value);
    }
}
