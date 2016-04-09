package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.view.MenuItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.backup.BackupFragment
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.catalogue.CatalogueFragment
import eu.kanade.tachiyomi.ui.download.DownloadFragment
import eu.kanade.tachiyomi.ui.library.LibraryFragment
import eu.kanade.tachiyomi.ui.recent.RecentChaptersFragment
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.setVectorCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : BaseActivity() {

    override fun onCreate(savedState: Bundle?) {
        setAppTheme()
        super.onCreate(savedState)

        // Do not let the launcher create a new activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }

        // Inflate activity_edit_categories.xml.
        setContentView(R.layout.activity_main)

        // Handle Toolbar
        setupToolbar(toolbar, backNavigation = false)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)

        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = android.R.color.transparent;
        }

        // Set behavior of Navigation drawer
        nav_view.setNavigationItemSelectedListener { item ->
            // Make information view invisible
            image_view.setImageResource(android.R.color.transparent)
            text_label.text = ""

            when (item.itemId) {
                R.id.nav_drawer_library -> setFragment(LibraryFragment.newInstance())
                R.id.nav_drawer_recent_updates -> setFragment(RecentChaptersFragment.newInstance())
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
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> drawer.openDrawer(GravityCompat.START)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit()
    }

    fun updateEmptyView(show: Boolean, textResource: Int, drawable: Int) {
        if (show) {
            image_view.setVectorCompat(drawable, theme.getResourceColor(android.R.attr.textColorHint))
            text_label.text = getString(textResource)
        } else {
            image_view.setImageResource(android.R.color.transparent)
            text_label.text = ""
        }
    }
}