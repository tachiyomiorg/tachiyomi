package eu.kanade.tachiyomi.ui.library

/**
 * event for handling view mode switches
 */

class libraryToggleViewEvent(val viewAsList: Boolean = false){

    fun isListView(): Boolean{
        return viewAsList;
    }

}