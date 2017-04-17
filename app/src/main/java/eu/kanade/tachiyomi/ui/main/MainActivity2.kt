package eu.kanade.tachiyomi.ui.main

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.view.ViewGroup
import com.bluelinelabs.conductor.*
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.NoToolbarElevationController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.library2.LibraryController
import eu.kanade.tachiyomi.ui.recent_updates.RecentChaptersController
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadController
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import eu.kanade.tachiyomi.util.gone
import eu.kanade.tachiyomi.util.visible
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.toolbar.*
import uy.kohesive.injekt.injectLazy


class MainActivity2 : BaseActivity() {

    private lateinit var router: Router

    val preferences: PreferencesHelper by injectLazy()

    private val startScreenId by lazy {
        when (preferences.startScreen()) {
            1 -> R.id.nav_drawer_library
            2 -> R.id.nav_drawer_recently_read
            3 -> R.id.nav_drawer_recent_updates
            else -> R.id.nav_drawer_library
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)

        // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
        if (!isTaskRoot) {
            finish()
            return
        }

        setContentView(R.layout.activity_main2)

        setSupportActionBar(toolbar)

        val drawerArrow = DrawerArrowDrawable(this)
        drawerArrow.color = Color.WHITE
        toolbar.navigationIcon = drawerArrow

        // Set behavior of Navigation drawer
        nav_view.setNavigationItemSelectedListener { item ->
            val id = item.itemId

            val currentRoot = router.backstack.firstOrNull()
            if (currentRoot?.tag()?.toInt() != id) {
                when (id) {
                    R.id.nav_drawer_library -> setRoot(LibraryController(), id)
                    R.id.nav_drawer_recent_updates -> setRoot(RecentChaptersController(), id)
                    R.id.nav_drawer_recently_read -> setRoot(RecentlyReadController(), id)
//                    R.id.nav_drawer_catalogues -> setFragment(CatalogueFragment.newInstance(), id)
//                    R.id.nav_drawer_latest_updates -> setFragment(LatestUpdatesFragment.newInstance(), id)
//                    R.id.nav_drawer_downloads -> startActivity(Intent(this, DownloadActivity::class.java))
                    R.id.nav_drawer_settings -> {
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivityForResult(intent, REQUEST_OPEN_SETTINGS)
                    }
                }
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }

        val container = findViewById(R.id.controller_container) as ViewGroup

        router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) {
            nav_view.setCheckedItem(startScreenId)
            nav_view.menu.performIdentifierAction(startScreenId, 0)
        }

        toolbar.setNavigationOnClickListener {
            if (router.backstackSize == 1) {
                drawer.openDrawer(GravityCompat.START)
            } else {
                onBackPressed()
            }
        }

        router.addChangeListener(object : ControllerChangeHandler.ControllerChangeListener {
            override fun onChangeStarted(to: Controller?, from: Controller?, isPush: Boolean,
                                         container: ViewGroup, handler: ControllerChangeHandler) {
                val showHamburger = router.backstackSize == 1
                if (showHamburger) {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                } else {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }

                ObjectAnimator.ofFloat(drawerArrow, "progress", if (showHamburger) 0f else 1f).start()

                if (to !is DialogController) {
                    if (to is TabbedController) {
                        tabs.visible()
                    } else {
                        tabs.gone()
                    }

                    if (to is NoToolbarElevationController) {
                        appbar.disableElevation()
                    } else {
                        appbar.enableElevation()
                    }
                }

            }

            override fun onChangeCompleted(to: Controller?, from: Controller?, isPush: Boolean,
                                           container: ViewGroup, handler: ControllerChangeHandler) {

            }

        })

    }

    private fun setRoot(controller: Controller, id: Int) {
        router.setRoot(RouterTransaction.with(controller)
                .popChangeHandler(FadeChangeHandler())
                .pushChangeHandler(FadeChangeHandler())
                .tag(id.toString()))
    }

    override fun onDestroy() {
        super.onDestroy()
        nav_view.setNavigationItemSelectedListener(null)
        toolbar.setNavigationOnClickListener(null)
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    companion object {
        private const val REQUEST_OPEN_SETTINGS = 200
        // Shortcut actions
        private const val SHORTCUT_LIBRARY = "eu.kanade.tachiyomi.SHOW_LIBRARY"
        private const val SHORTCUT_RECENTLY_UPDATED = "eu.kanade.tachiyomi.SHOW_RECENTLY_UPDATED"
        private const val SHORTCUT_RECENTLY_READ = "eu.kanade.tachiyomi.SHOW_RECENTLY_READ"
        private const val SHORTCUT_CATALOGUES = "eu.kanade.tachiyomi.SHOW_CATALOGUES"
    }

}