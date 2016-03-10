package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v14.preference.PreferenceFragment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import kotlinx.android.synthetic.main.toolbar.*
import javax.inject.Inject

class SettingsActivity : BaseActivity() {

    @Inject lateinit var preferences: PreferencesHelper
    @Inject lateinit var chapterCache: ChapterCache
    @Inject lateinit var db: DatabaseHelper
    @Inject lateinit var sourceManager: SourceManager
    @Inject lateinit var syncManager: MangaSyncManager

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setContentView(R.layout.activity_preferences)
        applicationComponent.inject(this)

        setupToolbar(toolbar)

        if (savedState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.settings_content, SettingsMainFragment())
                    .commit()
        }
    }

    override fun onBackPressed() {
        if (!fragmentManager.popBackStackImmediate()) {
            super.onBackPressed()
        }
    }

    class SettingsMainFragment : PreferenceFragment() {

        override fun onCreatePreferences(savedState: Bundle?, s: String?) {
            addPreferencesFromResource(R.xml.pref_main)

            registerSubpreference(R.string.pref_category_general_key) {
                SettingsGeneralFragment.newInstance(R.xml.pref_general, R.string.pref_category_general)
            }

            registerSubpreference(R.string.pref_category_reader_key) {
                SettingsNestedFragment.newInstance(R.xml.pref_reader, R.string.pref_category_reader)
            }

            registerSubpreference(R.string.pref_category_downloads_key) {
                SettingsDownloadsFragment.newInstance(R.xml.pref_downloads, R.string.pref_category_downloads)
            }

            registerSubpreference(R.string.pref_category_sources_key) {
                SettingsSourcesFragment.newInstance(R.xml.pref_sources, R.string.pref_category_sources)
            }

            registerSubpreference(R.string.pref_category_sync_key) {
                SettingsSyncFragment.newInstance(R.xml.pref_sync, R.string.pref_category_sync)
            }

            registerSubpreference(R.string.pref_category_advanced_key) {
                SettingsAdvancedFragment.newInstance(R.xml.pref_advanced, R.string.pref_category_advanced)
            }

            registerSubpreference(R.string.pref_category_about_key) {
                SettingsAboutFragment.newInstance(R.xml.pref_about, R.string.pref_category_about)
            }
        }

        override fun onResume() {
            super.onResume()
            (activity as BaseActivity).setToolbarTitle(getString(R.string.label_settings))
        }

        private fun registerSubpreference(preferenceResource: Int, func: () -> PreferenceFragment) {
            findPreference(getString(preferenceResource)).setOnPreferenceClickListener {
                val fragment = func()
                fragmentManager.beginTransaction()
                        .replace(R.id.settings_content, fragment)
                        .addToBackStack(fragment.javaClass.simpleName)
                        .commit()
                true
            }
        }

    }

}
