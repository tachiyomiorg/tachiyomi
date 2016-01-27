package eu.kanade.tachiyomi.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.App;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity;
import eu.kanade.tachiyomi.ui.reader.viewer.base.BaseReader;
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.LeftToRightReader;
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.RightToLeftReader;
import eu.kanade.tachiyomi.ui.reader.viewer.pager.vertical.VerticalReader;
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonReader;
import eu.kanade.tachiyomi.util.GLUtil;
import eu.kanade.tachiyomi.util.ToastUtil;
import icepick.Icepick;
import nucleus.factory.RequiresPresenter;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

@RequiresPresenter(ReaderPresenter.class)
public class ReaderActivity extends BaseRxActivity<ReaderPresenter> {

    @Bind(R.id.page_number) TextView pageNumber;
    @Bind(R.id.toolbar) Toolbar toolbar;

    @Inject PreferencesHelper preferences;

    private BaseReader viewer;
    private ReaderMenu readerMenu;

    private int uiFlags;
    private int readerTheme;
    protected CompositeSubscription subscriptions;
    private Subscription customBrightnessSubscription;

    private int maxBitmapSize;

    public static final int LEFT_TO_RIGHT = 1;
    public static final int RIGHT_TO_LEFT = 2;
    public static final int VERTICAL = 3;
    public static final int WEBTOON = 4;

    public static final int BLACK_THEME = 1;

    public static Intent newIntent(Context context) {
        return new Intent(context, ReaderActivity.class);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        App.get(this).getComponent().inject(this);
        setContentView(R.layout.activity_reader);
        ButterKnife.bind(this);

        setupToolbar(toolbar);
        subscriptions = new CompositeSubscription();

        readerMenu = new ReaderMenu(this);
        Icepick.restoreInstanceState(readerMenu, savedState);
        if (savedState != null && readerMenu.showing)
            readerMenu.show(false);

        initializeSettings();

        maxBitmapSize = GLUtil.getMaxTextureSize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setSystemUiVisibility();
    }

    @Override
    protected void onPause() {
        if (viewer != null)
            getPresenter().setCurrentPage(viewer.getCurrentPage());
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        subscriptions.unsubscribe();
        viewer = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return readerMenu.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return readerMenu.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Icepick.saveInstanceState(readerMenu, outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (viewer != null)
            getPresenter().setCurrentPage(viewer.getCurrentPage());
        getPresenter().onChapterLeft();

        int chapterToUpdate = getPresenter().getMangaSyncChapterToUpdate();

        if (chapterToUpdate > 0) {
            if (getPresenter().prefs.askUpdateMangaSync()) {
                new MaterialDialog.Builder(this)
                        .content(getString(R.string.confirm_update_manga_sync, chapterToUpdate))
                        .positiveText(R.string.button_yes)
                        .negativeText(R.string.button_no)
                        .onPositive((dialog, which) -> {
                            getPresenter().updateMangaSyncLastChapterRead();
                        })
                        .onAny((dialog1, which1) -> {
                            finish();
                        })
                        .show();
            } else {
                getPresenter().updateMangaSyncLastChapterRead();
                finish();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void onChapterError() {
        finish();
        ToastUtil.showShort(this, R.string.page_list_error);
    }

    public void onChapterReady(List<Page> pages, Manga manga, Chapter chapter, int currentPage) {
        if (currentPage == -1) {
            currentPage = pages.size() - 1;
        }

        if (viewer == null) {
            viewer = createViewer(manga);
            getSupportFragmentManager().beginTransaction().replace(R.id.reader, viewer).commit();
        }
        viewer.onPageListReady(pages, currentPage);
        readerMenu.onChapterReady(pages.size(), manga, chapter, currentPage);
    }

    public void onAdjacentChapters(Chapter previous, Chapter next) {
        readerMenu.onAdjacentChapters(previous, next);
    }

    private BaseReader createViewer(Manga manga) {
        int mangaViewer = manga.viewer == 0 ? preferences.getDefaultViewer() : manga.viewer;

        switch (mangaViewer) {
            case LEFT_TO_RIGHT: default:
                return new LeftToRightReader();
            case RIGHT_TO_LEFT:
                return new RightToLeftReader();
            case VERTICAL:
                return new VerticalReader();
            case WEBTOON:
                return new WebtoonReader();
        }
    }

    public void onPageChanged(int currentPageIndex, int totalPages) {
        String page = (currentPageIndex + 1) + "/" + totalPages;
        pageNumber.setText(page);
        readerMenu.onPageChanged(currentPageIndex);
    }

    public void setSelectedPage(int pageIndex) {
        viewer.setSelectedPage(pageIndex);
    }

    public void onCenterSingleTap() {
        readerMenu.toggle();
    }

    public void requestNextChapter() {
        getPresenter().setCurrentPage(viewer != null ? viewer.getCurrentPage() : 0);
        if (!getPresenter().loadNextChapter()) {
            ToastUtil.showShort(this, R.string.no_next_chapter);
        }

    }

    public void requestPreviousChapter() {
        getPresenter().setCurrentPage(viewer != null ? viewer.getCurrentPage() : 0);
        if (!getPresenter().loadPreviousChapter()) {
            ToastUtil.showShort(this, R.string.no_previous_chapter);
        }
    }

    private void initializeSettings() {
        subscriptions.add(preferences.showPageNumber()
                .asObservable()
                .subscribe(this::setPageNumberVisibility));

        subscriptions.add(preferences.lockOrientation()
                .asObservable()
                .subscribe(this::setOrientation));

        subscriptions.add(preferences.hideStatusBar()
                .asObservable()
                .subscribe(this::setStatusBarVisibility));

        subscriptions.add(preferences.keepScreenOn()
                .asObservable()
                .subscribe(this::setKeepScreenOn));

        subscriptions.add(preferences.customBrightness()
                .asObservable()
                .subscribe(this::setCustomBrightness));

        subscriptions.add(preferences.readerTheme()
                .asObservable()
                .distinctUntilChanged()
                .subscribe(this::applyTheme));
    }

    private void setOrientation(boolean locked) {
        if (locked) {
            int orientation;
            int rotation = ((WindowManager) getSystemService(
                    Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
            }
            setRequestedOrientation(orientation);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void setPageNumberVisibility(boolean visible) {
        pageNumber.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private void setKeepScreenOn(boolean enabled) {
        if (enabled) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setCustomBrightness(boolean enabled) {
        if (enabled) {
            subscriptions.add(customBrightnessSubscription = preferences.customBrightnessValue()
                    .asObservable()
                    .subscribe(this::setCustomBrightnessValue));
        } else {
            if (customBrightnessSubscription != null)
                subscriptions.remove(customBrightnessSubscription);
            setCustomBrightnessValue(-1);
        }
    }

    private void setCustomBrightnessValue(float value) {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = value;
        getWindow().setAttributes(layout);
    }

    private void setStatusBarVisibility(boolean hidden) {
        createUiHideFlags(hidden);
        setSystemUiVisibility();
    }

    private void createUiHideFlags(boolean statusBarHidden) {
        uiFlags = 0;
        uiFlags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (statusBarHidden)
            uiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            uiFlags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }

    public void setSystemUiVisibility() {
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }

    protected void setMangaDefaultViewer(int viewer) {
        getPresenter().updateMangaViewer(viewer);
        recreate();
    }

    private void applyTheme(int theme) {
        readerTheme = theme;
        View rootView = getWindow().getDecorView().getRootView();
        if (theme == BLACK_THEME) {
            rootView.setBackgroundColor(Color.BLACK);
            pageNumber.setTextColor(ContextCompat.getColor(this, R.color.light_grey));
            pageNumber.setBackgroundColor(ContextCompat.getColor(this, R.color.page_number_background_black));
        } else {
            rootView.setBackgroundColor(Color.WHITE);
            pageNumber.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
            pageNumber.setBackgroundColor(ContextCompat.getColor(this, R.color.page_number_background));
        }
    }

    public int getReaderTheme() {
        return readerTheme;
    }

    public PreferencesHelper getPreferences() {
        return preferences;
    }

    public BaseReader getViewer() {
        return viewer;
    }

    public int getMaxBitmapSize() {
        return maxBitmapSize;
    }

}
