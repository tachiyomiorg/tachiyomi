package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.Intent
import android.support.customtabs.CustomTabsIntent
import android.support.v7.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.TrackLoginDialog
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsTrackingController : SettingsController(),
        TrackLoginDialog.Listener {

    private val trackManager: TrackManager by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_tracking

        intListPreference {
            key = Keys.cover_source
            titleRes = R.string.pref_cover_source
            entriesRes = arrayOf(R.string.source_covers, R.string.myanimelist, R.string.anilist, R.string.kitsu)
            entryValues = arrayOf("0", "1", "2", "3")
            defaultValue = "0"
            summary = "%s"
        }

        switchPreference {
            key = Keys.autoUpdateTrack
            titleRes = R.string.pref_auto_update_manga_sync
            defaultValue = true
        }
        switchPreference {
            key = Keys.askUpdateTrack
            titleRes = R.string.pref_ask_update_manga_sync
            defaultValue = false
        }.apply {
            dependency = Keys.autoUpdateTrack // the preference needs to be attached.
        }
        preferenceCategory {
            titleRes = R.string.services

            trackPreference(trackManager.myAnimeList) {
                onClick {
                    val dialog = TrackLoginDialog(trackManager.myAnimeList)
                    dialog.targetController = this@SettingsTrackingController
                    dialog.showDialog(router)
                }
            }
            trackPreference(trackManager.aniList) {
                onClick {
                    val tabsIntent = CustomTabsIntent.Builder()
                            .setToolbarColor(context.getResourceColor(R.attr.colorPrimary))
                            .build()
                    tabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    tabsIntent.launchUrl(activity, AnilistApi.authUrl())
                }
            }
            trackPreference(trackManager.kitsu) {
                onClick {
                    val dialog = TrackLoginDialog(trackManager.kitsu)
                    dialog.targetController = this@SettingsTrackingController
                    dialog.showDialog(router)
                }
            }
        }
    }

    inline fun PreferenceScreen.trackPreference(
            service: TrackService,
            block: (@DSL LoginPreference).() -> Unit
    ): LoginPreference {
        return initThenAdd(LoginPreference(context).apply {
            key = Keys.trackUsername(service.id)
            title = service.name
        }, block)
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        // Manually refresh anilist holder
        updatePreference(trackManager.aniList.id)
    }

    private fun updatePreference(id: Int) {
        val pref = findPreference(Keys.trackUsername(id)) as? LoginPreference
        pref?.notifyChanged()
    }

    override fun trackDialogClosed(service: TrackService) {
        updatePreference(service.id)
    }

}