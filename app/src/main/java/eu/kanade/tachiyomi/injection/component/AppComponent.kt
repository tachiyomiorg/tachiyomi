package eu.kanade.tachiyomi.injection.component

import android.app.Application
import dagger.Component
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.glide.AppGlideModule
import eu.kanade.tachiyomi.data.glide.MangaModelLoader
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.mangasync.UpdateMangaSyncService
import eu.kanade.tachiyomi.data.mangasync.base.MangaSyncService
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.data.source.base.OnlineSource
import eu.kanade.tachiyomi.data.updater.UpdateDownloader
import eu.kanade.tachiyomi.injection.module.AppModule
import eu.kanade.tachiyomi.injection.module.DataModule
import eu.kanade.tachiyomi.ui.backup.BackupPresenter
import eu.kanade.tachiyomi.ui.catalogue.CataloguePresenter
import eu.kanade.tachiyomi.ui.category.CategoryPresenter
import eu.kanade.tachiyomi.ui.download.DownloadPresenter
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.ui.manga.MangaPresenter
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersPresenter
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoPresenter
import eu.kanade.tachiyomi.ui.manga.myanimelist.MyAnimeListPresenter
import eu.kanade.tachiyomi.ui.reader.ReaderPresenter
import eu.kanade.tachiyomi.ui.recent.RecentChaptersPresenter
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class, DataModule::class))
interface AppComponent {

    fun inject(libraryPresenter: LibraryPresenter)
    fun inject(mangaPresenter: MangaPresenter)
    fun inject(cataloguePresenter: CataloguePresenter)
    fun inject(mangaInfoPresenter: MangaInfoPresenter)
    fun inject(chaptersPresenter: ChaptersPresenter)
    fun inject(readerPresenter: ReaderPresenter)
    fun inject(downloadPresenter: DownloadPresenter)
    fun inject(myAnimeListPresenter: MyAnimeListPresenter)
    fun inject(categoryPresenter: CategoryPresenter)
    fun inject(recentChaptersPresenter: RecentChaptersPresenter)
    fun inject(backupPresenter: BackupPresenter)

    fun inject(mangaActivity: MangaActivity)
    fun inject(settingsActivity: SettingsActivity)

    fun inject(source: Source)
    fun inject(mangaSyncService: MangaSyncService)

    fun inject(onlineSource: OnlineSource)

    fun inject(libraryUpdateService: LibraryUpdateService)
    fun inject(downloadService: DownloadService)
    fun inject(updateMangaSyncService: UpdateMangaSyncService)

    fun inject(mangaModelLoader: MangaModelLoader)
    fun inject(appGlideModule: AppGlideModule)

    fun inject(updateDownloader: UpdateDownloader)
    fun application(): Application

}
