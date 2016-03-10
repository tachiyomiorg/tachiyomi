package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
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
    var prevIdentifier = -1L

    lateinit var drawer: Drawer

    lateinit var fragmentStack: FragmentStack

    val KEY_SELECTED_ITEM = "0x00000001"


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

        fragmentStack = FragmentStack(this, supportFragmentManager, R.id.frame_container
        ) { fragment ->
            if (fragment is ViewWithPresenter<*>)
                fragment.presenter.destroy()
        }

        var profile = ProfileDrawerItem().withIcon(ContextCompat.getDrawable(baseContext, R.drawable.test)).withIdentifier(100);

        var headerResult = AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .addProfiles(profile)
                .withSelectionListEnabled(false)
                .withProfileImagesClickable(false)
                .withHeaderBackground(ColorDrawable(resources.getColor(R.color.primary, theme)))
                .withSavedInstance(savedState)
                .build();

        drawer = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .withActionBarDrawerToggleAnimated(true)
                .withOnDrawerNavigationListener { view ->
                    if (fragmentStack.size() > 1) {
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

                            // Make information view invisible
                            image_view.setInformationDrawable(null)
                            text_label.text = ""

                        setIconToBlue(identifier)
                    }
                    false

                }
                .withSavedInstance(savedState)
                .build()

        if (savedState != null) {
            // Recover icon state after rotation
            if (fragmentStack.size() > 1) {
                showBackArrow()
            }

            // Set saved selection
            var identifier = savedState.getLong(KEY_SELECTED_ITEM)
            drawer.setSelection(identifier, false)
            setIconToBlue(identifier)
        } else {
            // Set default selection
            drawer.setSelection(R.id.nav_drawer_library.toLong())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_SELECTED_ITEM, drawer.currentSelection)
    }

    override fun onBackPressed() {
        if (!fragmentStack.pop()) {
            super.onBackPressed()
        } else if (fragmentStack.size() == 1) {
            showHamburgerIcon()
            drawer.actionBarDrawerToggle?.syncState()
        }
    }

    private fun showHamburgerIcon() {
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            drawer.actionBarDrawerToggle?.isDrawerIndicatorEnabled = true
            drawer.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }


    fun setFragment(fragment: Fragment) {
        fragmentStack.replace(fragment)
    }

    private fun setIconBackToGrey(prevIdentifier: Long, identifier: Long) {
        // Don't set to grey when settings
        if (identifier == R.id.nav_drawer_settings.toLong())
            return

            if (prevIdentifier == R.id.nav_drawer_library.toLong()) {
                drawer.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_book_grey_24dp)))
            } else if (prevIdentifier == R.id.nav_drawer_recent_updates.toLong()) {
                drawer.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_history_grey_24dp)))
            } else if (prevIdentifier == R.id.nav_drawer_catalogues.toLong()) {
                drawer.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_explore_grey_24dp)))
            } else if (prevIdentifier == R.id.nav_drawer_downloads.toLong()) {
                drawer.updateIcon(prevIdentifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_file_download_grey_24dp)))

        }

    }

    private fun setIconToBlue(identifier: Long) {
        if (identifier == R.id.nav_drawer_library.toLong()) {
            drawer.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_book_blue_24dp)))
            setFragment(LibraryFragment.newInstance())
        } else if (identifier == R.id.nav_drawer_recent_updates.toLong()) {
            drawer.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_history_blue_24dp)))
            setFragment(RecentChaptersFragment.newInstance())
        } else if (identifier == R.id.nav_drawer_catalogues.toLong()) {
            drawer.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_explore_blue_24dp)))
            setFragment(CatalogueFragment.newInstance())
        } else if (identifier == R.id.nav_drawer_downloads.toLong()) {
            drawer.updateIcon(identifier, ImageHolder(ContextCompat.getDrawable(this, R.drawable.ic_file_download_blue_24dp)));
            setFragment(DownloadFragment.newInstance());
        } else if (identifier == R.id.nav_drawer_settings.toLong()) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }


    private fun showBackArrow() {
        if (supportActionBar != null) {
            drawer.actionBarDrawerToggle?.isDrawerIndicatorEnabled = false
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            drawer.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }
}
