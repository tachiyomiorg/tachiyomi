package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.PreferenceCategory
import android.view.View
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.MangaSyncLoginDialog

class SettingsSyncFragment : SettingsNestedFragment() {

    companion object {
        const val SYNC_CHANGE_REQUEST = 121

        fun newInstance(resourcePreference: Int, resourceTitle: Int): SettingsNestedFragment {
            val fragment = SettingsSyncFragment()
            fragment.setArgs(resourcePreference, resourceTitle)
            return fragment
        }
    }

    val syncCategory by lazy { findPreference("pref_category_manga_sync_accounts") as PreferenceCategory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val themedContext = preferenceManager.context

        for (sync in settingsActivity.syncManager.services) {
            val pref = LoginPreference(themedContext).apply {
                key = PreferencesHelper.MANGASYNC_ACCOUNT_USERNAME + sync.id
                title = sync.name

                setOnPreferenceClickListener {
                    val fragment = MangaSyncLoginDialog.newInstance(sync)
                    fragment.setTargetFragment(this@SettingsSyncFragment, SYNC_CHANGE_REQUEST)
                    fragment.show(childFragmentManager, null)
                    true
                }
            }

            syncCategory.addPreference(pref)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SYNC_CHANGE_REQUEST) {
            val pref = findPreference(PreferencesHelper.MANGASYNC_ACCOUNT_USERNAME + resultCode) as? LoginPreference
            pref?.notifyChanged()
        }
    }

}
