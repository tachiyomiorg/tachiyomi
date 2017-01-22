package eu.kanade.tachiyomi.ui.latest_updates

import android.view.Menu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.catalogue.CatalogueFragment
import nucleus.factory.RequiresPresenter

/**
 * Fragment that shows the manga from the catalogue. Inherit CatalogueFragment.
 */
@RequiresPresenter(LatestUpdatesPresenter::class)
class LatestUpdatesFragment : CatalogueFragment() {

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_set_filter).isVisible = false

    }

    companion object {

        fun newInstance(): LatestUpdatesFragment {
            return LatestUpdatesFragment()
        }

    }

}