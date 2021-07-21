package eu.kanade.tachiyomi.ui.setting.database

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DatabaseSourcesPresenter(
    private val sourceManager: SourceManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get()
) : BasePresenter<DatabaseSourcesController>() {

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getAllManga()
            .asRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map(::findSourcesWithDatabaseEntries)
            .subscribeLatestCache(DatabaseSourcesController::setDatabaseSources)
    }

    private fun findSourcesWithDatabaseEntries(manga: List<Manga>): List<DatabaseSourceItem> {
        return manga
            .filterNot { it.favorite }
            .groupBy { it.source }
            .map { (sourceID, mangaList) ->
                val source = sourceManager.getOrStub(sourceID)
                DatabaseSourceItem(source, mangaList.size)
            }
            .sortedBy { it.source.name }
    }

    fun clearDatabaseForSources(sources: List<Long>) {
        db.deleteMangasNotInLibrary(sources).executeAsBlocking()
        db.deleteHistoryNoLastRead().executeAsBlocking()
    }
}
