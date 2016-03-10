package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v14.preference.PreferenceFragment
import eu.kanade.tachiyomi.data.preference.PreferencesHelper

open class SettingsNestedFragment : PreferenceFragment() {

    companion object {

        private val RESOURCE_FILE = "resource_file"
        private val TOOLBAR_TITLE = "toolbar_title"

        fun newInstance(resourcePreference: Int, resourceTitle: Int): SettingsNestedFragment {
            val fragment = SettingsNestedFragment()
            fragment.setArgs(resourcePreference, resourceTitle)
            return fragment
        }

    }

    override fun onCreatePreferences(savedState: Bundle?, s: String?) {
        addPreferencesFromResource(arguments.getInt(RESOURCE_FILE))
    }

    override fun onResume() {
        super.onResume()
        settingsActivity.setToolbarTitle(getString(arguments.getInt(TOOLBAR_TITLE)))
    }

    fun setArgs(resourcePreference: Int, resourceTitle: Int) {
        val args = Bundle()
        args.putInt(RESOURCE_FILE, resourcePreference)
        args.putInt(TOOLBAR_TITLE, resourceTitle)
        arguments = args
    }

    val settingsActivity: SettingsActivity
        get() = activity as SettingsActivity

    val preferences: PreferencesHelper
        get() = settingsActivity.preferences
}