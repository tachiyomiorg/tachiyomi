package eu.kanade.tachiyomi.ui.browse.extension.details

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcePreferencesPresenter(
    val sourceId: Long,
    sourceManager: SourceManager = Injekt.get()
) : BasePresenter<SourcePreferencesController>() {

    val source = sourceManager.get(sourceId)
}
