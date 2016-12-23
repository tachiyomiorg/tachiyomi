package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.TaskStackBuilder
import android.support.v4.view.GravityCompat
import android.view.MenuItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.backup.BackupFragment
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.catalogue.CatalogueFragment
import eu.kanade.tachiyomi.ui.download.DownloadFragment
import eu.kanade.tachiyomi.ui.latest_updates.LatestUpdatesFragment
import eu.kanade.tachiyomi.ui.library.LibraryFragment
import eu.kanade.tachiyomi.ui.recent_updates.RecentChaptersFragment
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadFragment
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import uy.kohesive.injekt.injectLazy

class MainActivity : BaseActivity() {

    val preferences: PreferencesHelper by injectLazy()

    private val startScreenId by lazy {
        when (preferences.startScreen()) {
            1 -> R.id.nav_drawer_library
            2 -> R.id.nav_drawer_recently_read
            3 -> R.id.nav_drawer_recent_updates
            else -> R.id.nav_drawer_library
        }
    }

    override fun onCreate(savedState: Bundle?) {
        setAppTheme()
        super.onCreate(savedState)

        // Do not let the launcher create a new activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }

        // Inflate activity_main.xml.
        setContentView(R.layout.activity_main)

        // Handle Toolbar
        setupToolbar(toolbar, backNavigation = false)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)

        // Set behavior of Navigation drawer
        nav_view.setNavigationItemSelectedListener { item ->
            // Make information view invisible
            empty_view.hide()

            val id = item.itemId
            when (id) {
                R.id.nav_drawer_library -> setFragment(LibraryFragment.newInstance(), id)
                R.id.nav_drawer_recent_updates -> setFragment(RecentChaptersFragment.newInstance(), id)
                R.id.nav_drawer_recently_read -> setFragment(RecentlyReadFragment.newInstance(), id)
                R.id.nav_drawer_catalogues -> setFragment(CatalogueFragment.newInstance(), id)
                R.id.nav_drawer_latest_updates -> setFragment(LatestUpdatesFragment.newInstance(), id)
                R.id.nav_drawer_downloads -> setFragment(DownloadFragment.newInstance(), id)
                R.id.nav_drawer_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivityForResult(intent, REQUEST_OPEN_SETTINGS)
                }
                R.id.nav_drawer_backup -> setFragment(BackupFragment.newInstance(), id)
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }

        if (savedState == null) {
            // Set start screen
            setSelectedDrawerItem(startScreenId)

            // Show changelog if needed
            ChangelogDialogFragment.show(this, preferences, supportFragmentManager)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> drawer.openDrawer(GravityCompat.START)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.frame_container)
        if (drawer.isDrawerOpen(GravityCompat.START) || drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawers()
        } else if (fragment != null && fragment.tag.toInt() != startScreenId) {
            if (resumed) {
                setSelectedDrawerItem(startScreenId)
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_OPEN_SETTINGS && resultCode != 0) {
            if (resultCode and SettingsActivity.FLAG_DATABASE_CLEARED != 0) {
                // If database is cleared avoid undefined behavior by recreating the stack.
                TaskStackBuilder.create(this)
                        .addNextIntent(Intent(this, MainActivity::class.java))
                        .startActivities()
            } else if (resultCode and SettingsActivity.FLAG_THEME_CHANGED != 0) {
                // Delay activity recreation to avoid fragment leaks.
                nav_view.post { recreate() }
            } else if (resultCode and SettingsActivity.FLAG_LANG_CHANGED != 0) {
                nav_view.post { recreate() }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setSelectedDrawerItem(itemId: Int, triggerAction: Boolean = true) {
        nav_view.setCheckedItem(itemId)
        if (triggerAction) {
            nav_view.menu.performIdentifierAction(itemId, 0)
        }
    }

    private fun setFragment(fragment: Fragment, itemId: Int) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.frame_container, fragment, "$itemId")
                .commit()
    }

    fun updateEmptyView(show: Boolean, textResource: Int, drawable: Int) {
        if (show) empty_view.show(drawable, textResource) else empty_view.hide()
    }

    companion object {
        private const val REQUEST_OPEN_SETTINGS = 200
    }
}
