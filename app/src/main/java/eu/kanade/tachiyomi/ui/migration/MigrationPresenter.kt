package eu.kanade.tachiyomi.ui.migration

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.combineLatest
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationPresenter(
    private val sourceManager: SourceManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get()
) : BasePresenter<MigrationController>() {

    var state = ViewState()
        private set(value) {
            field = value
            stateRelay.call(value)
        }

    private val stateRelay = BehaviorRelay.create(state)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getFavoriteMangas()
            .asRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { state = state.copy(sourcesWithManga = findSourcesWithManga(it)) }
            .combineLatest(
                stateRelay.map { it.selectedSource }
                    .distinctUntilChanged()
            ) { library, source -> library to source }
            .filter { (_, source) -> source != null }
            .observeOn(Schedulers.io())
            .map { (library, source) -> libraryToMigrationItem(library, source!!.id) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { state = state.copy(mangaForSource = it) }
            .subscribe()

        // Render the view when any field changes
        stateRelay.subscribeLatestCache(MigrationController::render)
    }

    fun setSelectedSource(source: Source) {
        state = state.copy(selectedSource = source, mangaForSource = emptyList())
    }

    fun deselectSource() {
        state = state.copy(selectedSource = null, mangaForSource = emptyList())
    }

    private fun findSourcesWithManga(library: List<Manga>): List<SourceItem> {
        val header = SelectionHeader()
        return library.map { it.source }.toSet()
            .mapNotNull { if (it != LocalSource.ID) sourceManager.getOrStub(it) else null }
            .sortedBy { it.name }
            .map { SourceItem(it, header) }
    }

    private fun libraryToMigrationItem(library: List<Manga>, sourceId: Long): List<MangaItem> {
        return library.filter { it.source == sourceId }.map(::MangaItem)
    }
}
