package eu.kanade.tachiyomi.ui.setting.search

import android.os.Bundle
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [SettingsSearchController]
 * Function calls should be done from here. UI calls should be done from the controller.
 */
open class SettingsSearchPresenter : BasePresenter<SettingsSearchController>() {

    val preferences: PreferencesHelper = Injekt.get()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        query = savedState?.getString(SettingsSearchPresenter::query.name) ?: "" // TODO - Some way to restore previous query?
    }

    override fun onSave(state: Bundle) {
        state.putString(SettingsSearchPresenter::query.name, query)
        super.onSave(state)
    }
}
