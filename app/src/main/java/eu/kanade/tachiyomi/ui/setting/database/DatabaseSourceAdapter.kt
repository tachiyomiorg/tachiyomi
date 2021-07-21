package eu.kanade.tachiyomi.ui.setting.database

import com.bluelinelabs.conductor.Controller
import eu.davidea.flexibleadapter.FlexibleAdapter

class DatabaseSourceAdapter(controller: Controller) :
    FlexibleAdapter<DatabaseSourceItem>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }
}
