package eu.kanade.tachiyomi.ui.catalogue.main

import eu.kanade.tachiyomi.source.CatalogueSource

class LastSourceItem(source: CatalogueSource) : SourceItem(source, null, 0, 1) {

    val id = ++counter

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            javaClass == other?.javaClass -> id == (other as LastSourceItem).id
            else -> false
        }
    }

    override fun hashCode(): Int {
        return source.hashCode() + id.hashCode()
    }

    companion object {
        var counter = 0
    }
}
