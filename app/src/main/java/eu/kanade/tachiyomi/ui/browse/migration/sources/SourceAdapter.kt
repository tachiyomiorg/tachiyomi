package eu.kanade.tachiyomi.ui.browse.migration.sources

import com.bluelinelabs.conductor.Controller
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [MigrationController].
 */
class SourceAdapter(controller: Controller) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }
}
