package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.Toolbar
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.catalogue.CatalogueFragment
import eu.kanade.tachiyomi.ui.download.DownloadFragment
import eu.kanade.tachiyomi.ui.library.LibraryFragment
import eu.kanade.tachiyomi.ui.recent.RecentChaptersFragment
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import eu.kanade.tachiyomi.util.setInformationDrawable
import icepick.State
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import nucleus.view.ViewWithPresenter

class MainActivity : BaseActivity() {
    var prevIdentifier = -1L
    var drawer: Drawer? = null
    private var fragmentStack: FragmentStack? = null

    @State
    var selectedItem: Long? = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not let the launcher create a new activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }

        // Inflate activity_edit_categories.xml.
        setContentView(R.layout.activity_main)

        // Handle Toolbar
        setupToolbar(toolbar)

        fragmentStack = FragmentStack(this, supportFragmentManager, R.id.frame_container
        ) { fragment ->
            if (fragment is ViewWithPresenter<*>)
                fragment.presenter.destroy()
        }

        var headerResult = AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withHeaderBackground(R.drawable.header)
                .withSavedInstance(savedInstanceState)
                .build();

        drawer = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .withActionBarDrawerToggleAnimated(true)
                .withOnDrawerNavigationListener { view ->
                    if ((fragmentStack as FragmentStack).size() > 1) {
                        onBackPressed()
                    }
                    false
                }
                .addDrawerItems(
                        PrimaryDrawerItem()
                                .withName(R.string.label_library)
                                .withIdentifier(R.id.nav_drawer_library.toLong())
                                .withIcon(ContextCompat.getDrawable(this, R.drawable.ic_book_grey_24dp)),
                        PrimaryDrawerItem()
                                .withName(R.string.label_recent_updates)
                                .withIdentifier(R.id.nav_drawer_recent_updates.toLong())
                                .withIcon(ContextCompat.getDrawable(this, R.drawable.ic_history_grey_24dp)),
                        PrimaryDrawerItem()
                                .withName(R.string.label_catalogues)
                                .withIdentifier(R.id.nav_drawer_catalogues.toLong())
                                .withIcon(ContextCompat.getDrawable(this, R.drawable.ic_explore_grey_24dp)),
                        PrimaryDrawerItem()
                                .withName(R.string.label_download_queue)
                                .withIdentifier(R.id.nav_drawer_downloads.toLong())
                                .withIcon(ContextCompat.getDrawable(this, R.drawable.ic_file_download_grey_24dp)),
                        DividerDrawerItem(),
                        PrimaryDrawerItem()
                                .withName(R.string.label_settings)
                                .withIdentifier(R.id.nav_drawer_settings.toLong())
                                .withSelectable(false)
                                .withIcon(ContextCompat.getDrawable(this, R.drawable.ic_settings_grey_24dp))
                )
                .withOnDrawerItemClickListener { view, position, drawerItem ->
                    drawerItem.let {
                        val identifier = drawerItem.identifier
                        if (prevIdentifier != -1L) {
                            setIconBackToGrey(prevIdentifier, identifier)
                        }
                        prevIdentifier = identifier

                        drawer?.let {
                            // Make information view invisible
                            image_view.setInformationDrawable(null)
                            text_label.text = ""

                            if (identifier == R.id.nav_drawer_library.toLong()) {
                                it.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_book_blue_24dp)))
                                setFragment(LibraryFragment.newInstance())
                            } else if (identifier == R.id.nav_drawer_recent_updates.toLong()) {
                                it.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_history_blue_24dp)))
                                setFragment(RecentChaptersFragment.newInstance())
                            } else if (identifier == R.id.nav_drawer_catalogues.toLong()) {
                                it.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_explore_blue_24dp)))
                                setFragment(CatalogueFragment.newInstance())
                            } else if (identifier == R.id.nav_drawer_downloads.toLong()) {
                                it.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_file_download_blue_24dp)));
                                setFragment(DownloadFragment.newInstance());
                            } else if (identifier == R.id.nav_drawer_settings.toLong()) {
                                startActivity(Intent(this, SettingsActivity::class.java))
                            }
                        }
                    }
                    false

                }
                .withSavedInstance(savedInstanceState)
                .build()

        if (savedInstanceState != null) {
            // Recover icon state after rotation
            if ((fragmentStack as FragmentStack).size() > 1) {
                showBackArrow()
            }

            // Set saved selection
            drawer?.setSelection(selectedItem as Long, false)
        } else {
            // Set default selection
            drawer?.setSelection(R.id.nav_drawer_library.toLong())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedItem = drawer?.currentSelection as Long
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (!(fragmentStack?.pop() as Boolean)) {
            super.onBackPressed()
        } else if (fragmentStack?.size() == 1) {
            showHamburgerIcon()
            drawer?.actionBarDrawerToggle?.syncState()
        }
    }

    private fun showHamburgerIcon() {
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            drawer?.actionBarDrawerToggle?.isDrawerIndicatorEnabled = true
            drawer?.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }


    fun setFragment(fragment: Fragment) {
        fragmentStack?.replace(fragment)
    }

    private fun setIconBackToGrey(prevIdentifier: Long, identifier: Long) {
        // Don't set to grey when settings
        if (identifier == R.id.nav_drawer_settings.toLong())
            return

        drawer?.let {
            if (prevIdentifier == R.id.nav_drawer_library.toLong()) {
                it.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_book_grey_24dp)))
            } else if (prevIdentifier == R.id.nav_drawer_recent_updates.toLong()) {
                it.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_history_grey_24dp)))
            } else if (prevIdentifier == R.id.nav_drawer_catalogues.toLong()) {
                it.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_explore_grey_24dp)))
            } else if (prevIdentifier == R.id.nav_drawer_downloads.toLong()) {
                it.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_file_download_grey_24dp)))
            }
        }

    }

    fun getToolbar(): Toolbar {
        return toolbar
    }

    /**
     * TODO
     */
    fun getAppBar(): AppBarLayout? {
        return appbar
    }

    private fun showBackArrow() {
        if (supportActionBar != null) {
            drawer?.actionBarDrawerToggle?.isDrawerIndicatorEnabled = false
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            drawer?.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }
}
