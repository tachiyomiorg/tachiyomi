package eu.kanade.tachiyomi.ui.catalogue_new

import eu.davidea.flexibleadapter.FlexibleAdapter

class CatalogueAdapter(listener: OnItemClickListener) :
        FlexibleAdapter<CatalogueItem>(null, listener, true)