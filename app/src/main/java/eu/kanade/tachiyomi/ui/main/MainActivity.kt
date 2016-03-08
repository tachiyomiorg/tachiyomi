package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.view.MenuItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.catalogue.CatalogueFragment
import eu.kanade.tachiyomi.ui.download.DownloadFragment
import eu.kanade.tachiyomi.ui.library.LibraryFragment
import eu.kanade.tachiyomi.ui.recent.RecentChaptersFragment
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import eu.kanade.tachiyomi.util.setInformationDrawable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import nucleus.view.ViewWithPresenter

class MainActivity : BaseActivity() {
    lateinit var fragmentStack: FragmentStack


    override fun onCreate(savedState: Bundle?) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedState)

        // Do not let the launcher create a new activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }

        // Inflate activity_edit_categories.xml.
        setContentView(R.layout.activity_main)

        // Handle Toolbar
        setupToolbar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)

        fragmentStack = FragmentStack(this, supportFragmentManager, R.id.frame_container
        ) { fragment ->
            if (fragment is ViewWithPresenter<*>)
                fragment.presenter.destroy()
        }

        // Set behavior of Navigation drawer
        nav_view.setNavigationItemSelectedListener(
                { menuItem ->
                    // Make information view invisible
                    image_view.setInformationDrawable(null)
                    text_label.text = ""

                    when (menuItem.itemId) {
                        R.id.nav_drawer_library -> {
                            setFragment(LibraryFragment.newInstance())
                            menuItem.isChecked = true
                            drawer.closeDrawer(GravityCompat.START)
                        }
                        R.id.nav_drawer_recent_updates -> {
                            setFragment(RecentChaptersFragment.newInstance())
                            menuItem.isChecked = true
                            drawer.closeDrawer(GravityCompat.START)
                        }
                        R.id.nav_drawer_catalogues -> {
                            setFragment(CatalogueFragment.newInstance())
                            menuItem.isChecked = true
                            drawer.closeDrawer(GravityCompat.START)
                        }
                        R.id.nav_drawer_downloads -> {
                            setFragment(DownloadFragment.newInstance())
                            menuItem.isChecked = true
                            drawer.closeDrawer(GravityCompat.START)
                        }
                        R.id.nav_drawer_settings -> {
                            menuItem.isChecked = true
                            startActivity(Intent(this, SettingsActivity::class.java))
                            drawer.closeDrawer(GravityCompat.START)
                        }
                    }
                    true
                })

        setFragment(LibraryFragment.newInstance())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawer.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    fun setFragment(fragment: Fragment) {
        fragmentStack.replace(fragment)
    }
}
