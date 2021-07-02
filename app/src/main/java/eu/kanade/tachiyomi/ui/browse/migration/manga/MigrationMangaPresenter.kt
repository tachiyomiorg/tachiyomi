package eu.kanade.tachiyomi.ui.browse.migration.manga

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationMangaPresenter(
    private val sourceId: Long,
    private val db: DatabaseHelper = Injekt.get()
) : BasePresenter<MigrationMangaController>() {

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getFavoriteMangas()
            .asRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { libraryToMigrationItem(it) }
            .subscribeLatestCache(MigrationMangaController::setManga)
    }

    private fun libraryToMigrationItem(library: List<Manga>): List<MigrationMangaItem> {
        return library.filter { it.source == sourceId }
            .sortedBy { it.title }
            .map { MigrationMangaItem(it) }
    }
}
