package eu.kanade.tachiyomi.ui.manga;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.App;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity;
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersFragment;
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoFragment;
import eu.kanade.tachiyomi.ui.manga.myanimelist.MyAnimeListFragment;
import nucleus.factory.RequiresPresenter;

@RequiresPresenter(MangaPresenter.class)
public class MangaActivity extends BaseRxActivity<MangaPresenter> {

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.tabs) TabLayout tabs;
    @Bind(R.id.view_pager) ViewPager view_pager;

    @Inject PreferencesHelper preferences;
    @Inject MangaSyncManager mangaSyncManager;

    private MangaDetailAdapter adapter;
    private long manga_id;
    private boolean is_online;

    public final static String MANGA_ID = "manga_id";
    public final static String MANGA_ONLINE = "manga_online";

    public static Intent newIntent(Context context, Manga manga) {
        Intent intent = new Intent(context, MangaActivity.class);
        intent.putExtra(MANGA_ID, manga.id);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        App.get(this).getComponent().inject(this);
        setContentView(R.layout.activity_manga);
        ButterKnife.bind(this);

        setupToolbar(toolbar);

        Intent intent = getIntent();

        manga_id = intent.getLongExtra(MANGA_ID, -1);
        is_online = intent.getBooleanExtra(MANGA_ONLINE, false);

        setupViewPager();

        if (savedState == null)
            getPresenter().queryManga(manga_id);
    }

    private void setupViewPager() {
        adapter = new MangaDetailAdapter(getSupportFragmentManager(), this);

        view_pager.setAdapter(adapter);
        tabs.setupWithViewPager(view_pager);

        if (!is_online)
            view_pager.setCurrentItem(MangaDetailAdapter.CHAPTERS_FRAGMENT);
    }

    public void setManga(Manga manga) {
        setToolbarTitle(manga.title);
    }

    public boolean isCatalogueManga() {
        return is_online;
    }

    class MangaDetailAdapter extends FragmentPagerAdapter {

        private int pageCount;
        private String tabTitles[];

        final static int INFO_FRAGMENT = 0;
        final static int CHAPTERS_FRAGMENT = 1;
        final static int MYANIMELIST_FRAGMENT = 2;

        public MangaDetailAdapter(FragmentManager fm, Context context) {
            super(fm);
            tabTitles = new String[]{
                    context.getString(R.string.manga_detail_tab),
                    context.getString(R.string.manga_chapters_tab),
                    "MAL"
            };

            pageCount = 2;
            if (!is_online && mangaSyncManager.getMyAnimeList().isLogged())
                pageCount++;
        }

        @Override
        public int getCount() {
            return pageCount;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case INFO_FRAGMENT:
                    return MangaInfoFragment.newInstance();
                case CHAPTERS_FRAGMENT:
                    return ChaptersFragment.newInstance();
                case MYANIMELIST_FRAGMENT:
                    return MyAnimeListFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate title based on item position
            return tabTitles[position];
        }
    }

}
