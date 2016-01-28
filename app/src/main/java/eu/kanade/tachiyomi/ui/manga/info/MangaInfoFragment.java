package eu.kanade.tachiyomi.ui.manga.info;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.model.LazyHeaders;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.cache.CoverCache;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.io.IOHandler;
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment;
import eu.kanade.tachiyomi.util.ToastUtil;
import nucleus.factory.RequiresPresenter;

@RequiresPresenter(MangaInfoPresenter.class)
public class MangaInfoFragment extends BaseRxFragment<MangaInfoPresenter> {

    private static final int REQUEST_IMAGE_OPEN = 1;
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
    @Bind(R.id.fab_edit) FloatingActionButton fabEdit;

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

        //Create edit drawable with size 24dp (google guidelines)
        IconicsDrawable edit = new IconicsDrawable(this.getContext())
                .icon(GoogleMaterial.Icon.gmd_edit)
                .color(ContextCompat.getColor(this.getContext(), R.color.white))
                .sizeDp(24);

        // Update image of fab buttons
        fabEdit.setImageDrawable(edit);

        // Set listener.
        fabEdit.setOnClickListener(v -> MangaInfoFragment.this.selectImage());

        favoriteBtn.setOnClickListener(v -> getPresenter().toggleFavorite());

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

    /**
     * Set the info of the manga
     *
     * @param manga       manga object containing information about manga
     * @param mangaSource the source of the manga
     */
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
                coverCache.saveOrLoadFromCache(cover, manga.thumbnail_url, headers);
            } else {
                coverCache.loadFromNetwork(cover, manga.thumbnail_url, headers);
            }
        }
    }

    public void setChapterCount(int count) {
        chapterCount.setText(String.valueOf(count));
    }

    private void setFavoriteText(boolean isFavorite) {
        favoriteBtn.setText(!isFavorite ? R.string.add_to_library : R.string.remove_from_library);
    }

    private void fetchMangaFromSource() {
        setRefreshing(true);
        getPresenter().fetchMangaFromSource();
    }

    private void selectImage() {
        if (getPresenter().getManga().favorite) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            // Do this if you need to be able to open the returned URI as a stream
            // (for example here to read the image data).
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            Intent finalIntent = Intent.createChooser(intent, getResources().getString(R.string.file_select_cover));

            startActivityForResult(finalIntent, REQUEST_IMAGE_OPEN);
        } else {
            ToastUtil.showShort(getContext(), R.string.notification_first_add_to_library);
        }

    }


    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_OPEN && resultCode == Activity.RESULT_OK) {
            // Get the file's content URI from the incoming Intent
            Uri returnUri = data.getData();

            // Convert to absolute path to prevent FileNotFoundException
            String result = IOHandler.getFilePath(this.getContext(), returnUri);

            // Get file from filepath
            File picture = new File(result);


            try {
                // Update cover to selected file
                getPresenter().editCoverWithLocalFile(picture, cover);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
