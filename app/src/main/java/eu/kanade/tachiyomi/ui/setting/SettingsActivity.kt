package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceScreen
import android.view.MenuItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import kotlinx.android.synthetic.main.toolbar.*
import net.xpece.android.support.preference.PreferenceScreenNavigationStrategy
import net.xpece.android.support.preference.PreferenceScreenNavigationStrategy.ReplaceFragment

class SettingsActivity : BaseActivity(),
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        PreferenceScreenNavigationStrategy.ReplaceFragment.Callbacks {

    private lateinit var replaceFragmentStrategy: ReplaceFragment

    /**
     * Flags to send to the parent activity for reacting to preference changes.
     */
    var parentFlags = 0
        set(value) {
            field = field or value
            setResult(field)
        }

    override fun onCreate(savedState: Bundle?) {
        setAppTheme()
        super.onCreate(savedState)
        setContentView(R.layout.activity_preferences)

        replaceFragmentStrategy = ReplaceFragment(this,
                R.anim.abc_fade_in, R.anim.abc_fade_out,
                R.anim.abc_fade_in, R.anim.abc_fade_out)

        if (savedState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.settings_content, SettingsFragment.newInstance(null), "Settings")
                .commit()
        } else {
            parentFlags = savedState.getInt(SettingsActivity::parentFlags.name)
        }

        setupToolbar(toolbar, backNavigation = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SettingsActivity::parentFlags.name, parentFlags)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBuildPreferenceFragment(key: String?): PreferenceFragmentCompat {
        return when (key) {
            "general_screen" -> SettingsGeneralFragment.newInstance(key)
            "downloads_screen" -> SettingsDownloadsFragment.newInstance(key)
            "sources_screen" -> SettingsSourcesFragment.newInstance(key)
            "tracking_screen" -> SettingsTrackingFragment.newInstance(key)
            "advanced_screen" -> SettingsAdvancedFragment.newInstance(key)
            "about_screen" -> SettingsAboutFragment.newInstance(key)
            else -> SettingsFragment.newInstance(key)
        }
    }

    override fun onPreferenceStartScreen(p0: PreferenceFragmentCompat, p1: PreferenceScreen): Boolean {
        replaceFragmentStrategy.onPreferenceStartScreen(supportFragmentManager, p0, p1)
        return true
    }

    companion object {
        const val FLAG_THEME_CHANGED = 0x1
        const val FLAG_DATABASE_CLEARED = 0x2
        const val FLAG_LANG_CHANGED = 0x4
    }

}
