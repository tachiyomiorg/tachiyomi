package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.data.mangasync.MangaSyncService
import eu.kanade.tachiyomi.data.mangasync.anilist.AnilistApi
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.MangaSyncLoginDialog
import uy.kohesive.injekt.injectLazy

class SettingsSyncFragment : SettingsFragment() {

    companion object {
        const val SYNC_CHANGE_REQUEST = 121

        fun newInstance(rootKey: String): SettingsSyncFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsSyncFragment().apply { arguments = args }
        }
    }

    private val syncManager: MangaSyncManager by injectLazy()

    private val preferences: PreferencesHelper by injectLazy()

    val syncCategory: PreferenceCategory by bindPref(R.string.pref_category_manga_sync_accounts_key)

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        registerService(syncManager.myAnimeList)

        registerService(syncManager.aniList) {
            val intent = CustomTabsIntent.Builder()
                    .setToolbarColor(activity.theme.getResourceColor(R.attr.colorPrimary))
                    .build()
            intent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent.launchUrl(activity, AnilistApi.authUrl())
        }
    }

    private fun <T : MangaSyncService> registerService(
            service: T,
            onPreferenceClick: (T) -> Unit = defaultOnPreferenceClick) {

        LoginPreference(preferenceManager.context).apply {
            key = preferences.keys.syncUsername(service.id)
            title = service.name

            setOnPreferenceClickListener {
                onPreferenceClick(service)
                true
            }

            syncCategory.addPreference(this)
        }
    }

    private val defaultOnPreferenceClick: (MangaSyncService) -> Unit
        get() = {
            val fragment = MangaSyncLoginDialog.newInstance(it)
            fragment.setTargetFragment(this, SYNC_CHANGE_REQUEST)
            fragment.show(fragmentManager, null)
        }

    override fun onResume() {
        super.onResume()
        // Manually refresh anilist holder
//        updatePreference(syncManager.aniList.id)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SYNC_CHANGE_REQUEST) {
            updatePreference(resultCode)
        }
    }

    private fun updatePreference(id: Int) {
        val pref = findPreference(preferences.keys.syncUsername(id)) as? LoginPreference
        pref?.notifyChanged()
    }

}
