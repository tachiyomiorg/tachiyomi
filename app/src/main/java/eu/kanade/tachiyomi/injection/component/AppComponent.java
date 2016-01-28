package eu.kanade.tachiyomi.injection.component;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Component;
import eu.kanade.tachiyomi.data.download.DownloadService;
import eu.kanade.tachiyomi.data.mangasync.services.MyAnimeList;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.sync.LibraryUpdateService;
import eu.kanade.tachiyomi.data.sync.UpdateMangaSyncService;
import eu.kanade.tachiyomi.injection.module.AppModule;
import eu.kanade.tachiyomi.injection.module.DataModule;
import eu.kanade.tachiyomi.ui.catalogue.CataloguePresenter;
import eu.kanade.tachiyomi.ui.download.DownloadPresenter;
import eu.kanade.tachiyomi.ui.library.LibraryPresenter;
import eu.kanade.tachiyomi.ui.library.category.CategoryPresenter;
import eu.kanade.tachiyomi.ui.manga.MangaActivity;
import eu.kanade.tachiyomi.ui.manga.MangaPresenter;
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersPresenter;
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoPresenter;
import eu.kanade.tachiyomi.ui.manga.myanimelist.MyAnimeListPresenter;
import eu.kanade.tachiyomi.ui.reader.ReaderActivity;
import eu.kanade.tachiyomi.ui.reader.ReaderPresenter;
import eu.kanade.tachiyomi.ui.recent.RecentChaptersPresenter;
import eu.kanade.tachiyomi.ui.setting.SettingsAccountsFragment;
import eu.kanade.tachiyomi.ui.setting.SettingsActivity;

@Singleton
@Component(
        modules = {
                AppModule.class,
                DataModule.class
        }
)
public interface AppComponent {

    void inject(LibraryPresenter libraryPresenter);
    void inject(MangaPresenter mangaPresenter);
    void inject(CataloguePresenter cataloguePresenter);
    void inject(MangaInfoPresenter mangaInfoPresenter);
    void inject(ChaptersPresenter chaptersPresenter);
    void inject(ReaderPresenter readerPresenter);
    void inject(DownloadPresenter downloadPresenter);
    void inject(MyAnimeListPresenter myAnimeListPresenter);
    void inject(CategoryPresenter categoryPresenter);
    void inject(RecentChaptersPresenter recentChaptersPresenter);

    void inject(ReaderActivity readerActivity);
    void inject(MangaActivity mangaActivity);
    void inject(SettingsAccountsFragment settingsAccountsFragment);
    void inject(SettingsActivity settingsActivity);

    void inject(Source source);

    void inject(MyAnimeList myAnimeList);

    void inject(LibraryUpdateService libraryUpdateService);
    void inject(DownloadService downloadService);
    void inject(UpdateMangaSyncService updateMangaSyncService);
    Application application();

}
