package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.TrackLoginDialog
import uy.kohesive.injekt.injectLazy

class SettingsTrackingFragment : SettingsFragment() {

    companion object {
        const val SYNC_CHANGE_REQUEST = 121

        fun newInstance(rootKey: String): SettingsTrackingFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsTrackingFragment().apply { arguments = args }
        }
    }

    private val trackManager: TrackManager by injectLazy()

    private val preferences: PreferencesHelper by injectLazy()

    val syncCategory: PreferenceCategory by bindPref(R.string.pref_category_tracking_accounts_key)

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        registerService(trackManager.myAnimeList)

        registerService(trackManager.aniList) {
            val intent = CustomTabsIntent.Builder()
                    .setToolbarColor(activity.theme.getResourceColor(R.attr.colorPrimary))
                    .build()
            intent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent.launchUrl(activity, AnilistApi.authUrl())
        }

        registerService(trackManager.kitsu)
    }

    private fun <T : TrackService> registerService(
            service: T,
            onPreferenceClick: (T) -> Unit = defaultOnPreferenceClick) {

        LoginPreference(preferenceManager.context).apply {
            key = preferences.keys.trackUsername(service.id)
            title = service.name

            setOnPreferenceClickListener {
                onPreferenceClick(service)
                true
            }

            syncCategory.addPreference(this)
        }
    }

    private val defaultOnPreferenceClick: (TrackService) -> Unit
        get() = {
            val fragment = TrackLoginDialog.newInstance(it)
            fragment.setTargetFragment(this, SYNC_CHANGE_REQUEST)
            fragment.show(fragmentManager, null)
        }

    override fun onResume() {
        super.onResume()
        // Manually refresh anilist holder
        updatePreference(trackManager.aniList.id)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SYNC_CHANGE_REQUEST) {
            updatePreference(resultCode)
        }
    }

    private fun updatePreference(id: Int) {
        val pref = findPreference(preferences.keys.trackUsername(id)) as? LoginPreference
        pref?.notifyChanged()
    }

}
