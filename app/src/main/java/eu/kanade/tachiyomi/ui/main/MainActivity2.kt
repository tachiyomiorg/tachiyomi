package eu.kanade.tachiyomi.ui.main

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.view.ViewGroup
import com.bluelinelabs.conductor.*
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.library2.LibraryController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*


class MainActivity2 : BaseActivity() {

    private lateinit var router: Router

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

        val container = findViewById(R.id.controller_container) as ViewGroup

        router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(LibraryController()))
        }

        toolbar.setNavigationOnClickListener {
            if (router.backstackSize == 1) {
                drawer.openDrawer(GravityCompat.START)
            } else {
                onBackPressed()
            }
        }

        router.addChangeListener(object : ControllerChangeHandler.ControllerChangeListener {
            override fun onChangeStarted(to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) {

            }

            override fun onChangeCompleted(to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) {
                val showHamburger = router.backstackSize == 1
                if (showHamburger) {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                } else {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }

                ObjectAnimator.ofFloat(drawerArrow, "progress", if (showHamburger) 0f else 1f).start()
            }

        })

        // Set behavior of Navigation drawer
        nav_view.setNavigationItemSelectedListener { item ->
            val id = item.itemId

            val oldFragment = supportFragmentManager.findFragmentById(R.id.frame_container)
            if (oldFragment == null || oldFragment.tag.toInt() != id) {
                when (id) {
                    R.id.nav_drawer_library -> router.setRoot(RouterTransaction.with(LibraryController()))
//                    R.id.nav_drawer_recent_updates -> router.replaceTopController(RouterTransaction.with(RecentUpdatesController()))
//                    R.id.nav_drawer_recently_read -> setFragment(RecentlyReadFragment.newInstance(), id)
//                    R.id.nav_drawer_catalogues -> setFragment(CatalogueFragment.newInstance(), id)
//                    R.id.nav_drawer_latest_updates -> setFragment(LatestUpdatesFragment.newInstance(), id)
//                    R.id.nav_drawer_downloads -> startActivity(Intent(this, DownloadActivity::class.java))
//                    R.id.nav_drawer_settings -> {
//                        val intent = Intent(this, SettingsActivity::class.java)
//                        startActivityForResult(intent, MainActivity.REQUEST_OPEN_SETTINGS)
//                    }
                }
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }
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

}