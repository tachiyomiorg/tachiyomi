package eu.kanade.tachiyomi.ui.browse.migration.sources

import com.bluelinelabs.conductor.Controller
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [MigrationController].
 */
class SourceAdapter(val controller: Controller) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val cardBackground = controller.activity!!.getResourceColor(R.attr.colorSurface)

    init {
        setDisplayHeadersAtStartUp(true)
    }
}
