package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.view.MenuItem
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.backup.BackupFragment
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.catalogue.CatalogueFragment
import eu.kanade.tachiyomi.ui.download.DownloadFragment
import eu.kanade.tachiyomi.ui.library.LibraryFragment
import eu.kanade.tachiyomi.ui.recent_updates.RecentChaptersFragment
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadFragment
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import javax.inject.Inject

class MainActivity : BaseActivity() {

    @Inject lateinit var preferences: PreferencesHelper

    override fun onCreate(savedState: Bundle?) {
        setAppTheme()
        super.onCreate(savedState)

        // Do not let the launcher create a new activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }

        App.get(this).component.inject(this)

        // Inflate activity_main.xml.
        setContentView(R.layout.activity_main)

        // Handle Toolbar
        setupToolbar(toolbar, backNavigation = false)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)

        // Set behavior of Navigation drawer
        nav_view.setNavigationItemSelectedListener { item ->
            // Make information view invisible
            empty_view.hide()

            when (item.itemId) {
                R.id.nav_drawer_library -> setFragment(LibraryFragment.newInstance())
                R.id.nav_drawer_recent_updates -> setFragment(RecentChaptersFragment.newInstance())
                R.id.nav_drawer_recent_manga -> setFragment(RecentlyReadFragment.newInstance())
                R.id.nav_drawer_catalogues -> setFragment(CatalogueFragment.newInstance())
                R.id.nav_drawer_downloads -> setFragment(DownloadFragment.newInstance())
                R.id.nav_drawer_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_drawer_backup -> setFragment(BackupFragment.newInstance())
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }

        if (savedState == null) {
            setFragment(LibraryFragment.newInstance())
            ChangelogDialogFragment.show(preferences, supportFragmentManager)
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
        supportFragmentManager.findFragmentById(R.id.frame_container)?.let {
            if (it !is LibraryFragment) {
                nav_view.setCheckedItem(R.id.nav_drawer_library)
                nav_view.menu.performIdentifierAction(R.id.nav_drawer_library, 0)
            } else {
                super.onBackPressed()
            }
        } ?: super.onBackPressed()
    }

    fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit()
    }

    fun updateEmptyView(show: Boolean, textResource: Int, drawable: Int) {
        if (show) empty_view.show(drawable, textResource) else empty_view.hide()
    }
}