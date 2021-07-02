package eu.kanade.tachiyomi.ui.browse.migration.manga

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

class MigrationMangaAdapter(controller: MigrationMangaController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val coverClickListener: OnCoverClickListener = controller

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
