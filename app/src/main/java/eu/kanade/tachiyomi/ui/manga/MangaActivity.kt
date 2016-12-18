package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.widget.LinearLayout
import android.widget.TextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersFragment
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoFragment
import eu.kanade.tachiyomi.ui.manga.track.TrackFragment
import eu.kanade.tachiyomi.util.SharedData
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.activity_manga.*
import kotlinx.android.synthetic.main.toolbar.*
import nucleus.factory.RequiresPresenter

@RequiresPresenter(MangaPresenter::class)
class MangaActivity : BaseRxActivity<MangaPresenter>() {

    companion object {

        const val FROM_CATALOGUE_EXTRA = "from_catalogue"
        const val MANGA_EXTRA = "manga"
        const val FROM_LAUNCHER_EXTRA = "from_launcher"
        const val INFO_FRAGMENT = 0
        const val CHAPTERS_FRAGMENT = 1
        const val TRACK_FRAGMENT = 2

        fun newIntent(context: Context, manga: Manga, fromCatalogue: Boolean = false): Intent {
            SharedData.put(MangaEvent(manga))
            return Intent(context, MangaActivity::class.java).apply {
                putExtra(FROM_CATALOGUE_EXTRA, fromCatalogue)
                putExtra(MANGA_EXTRA, manga.id)
            }
        }
    }

    private lateinit var adapter: MangaDetailAdapter

    var fromCatalogue: Boolean = false
        private set

    override fun onCreate(savedState: Bundle?) {
        setAppTheme()
        super.onCreate(savedState)
        setContentView(R.layout.activity_manga)

        val fromLauncher = intent.getBooleanExtra(FROM_LAUNCHER_EXTRA, false)

        // Remove any current manga if we are launching from launcher
        if (fromLauncher) SharedData.remove(MangaEvent::class.java)

        presenter.setMangaEvent(SharedData.getOrPut(MangaEvent::class.java) {
            val id = intent.getLongExtra(MANGA_EXTRA, 0)
            val dbManga = presenter.db.getManga(id).executeAsBlocking()
            if (dbManga != null) {
                MangaEvent(dbManga)
            } else {
                toast(R.string.manga_not_in_db)
                finish()
                return
            }
        })

        setupToolbar(toolbar)

        fromCatalogue = intent.getBooleanExtra(FROM_CATALOGUE_EXTRA, false)

        adapter = MangaDetailAdapter(supportFragmentManager, this)
        view_pager.offscreenPageLimit = 3
        view_pager.adapter = adapter

        tabs.setupWithViewPager(view_pager)

        if (!fromCatalogue)
            view_pager.currentItem = CHAPTERS_FRAGMENT

        requestPermissionsOnMarshmallow()
    }

    fun onSetManga(manga: Manga) {
        setToolbarTitle(manga.title)
    }

    fun setTrackingIcon(visible: Boolean) {
        val tab = tabs.getTabAt(TRACK_FRAGMENT) ?: return
        val drawable = if (visible)
            VectorDrawableCompat.create(resources, R.drawable.ic_done_white_18dp, null)
        else null

        // I had no choice but to use reflection...
        val field = tab.javaClass.getDeclaredField("mView").apply { isAccessible = true }
        val view = field.get(tab) as LinearLayout
        val textView = view.getChildAt(1) as TextView
        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        textView.compoundDrawablePadding = 4
    }

    private class MangaDetailAdapter(fm: FragmentManager, activity: MangaActivity)
    : FragmentPagerAdapter(fm) {

        private var tabCount = 2

        private val tabTitles = listOf(
                R.string.manga_detail_tab,
                R.string.manga_chapters_tab,
                R.string.manga_tracking_tab)
                .map { activity.getString(it) }

        init {
            if (!activity.fromCatalogue && activity.presenter.trackManager.hasLoggedServices())
                tabCount++
        }

        override fun getCount(): Int {
            return tabCount
        }

        override fun getItem(position: Int): Fragment {
            when (position) {
                INFO_FRAGMENT -> return MangaInfoFragment.newInstance()
                CHAPTERS_FRAGMENT -> return ChaptersFragment.newInstance()
                TRACK_FRAGMENT -> return TrackFragment.newInstance()
                else -> throw Exception("Unknown position")
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabTitles[position]
        }

    }

}
