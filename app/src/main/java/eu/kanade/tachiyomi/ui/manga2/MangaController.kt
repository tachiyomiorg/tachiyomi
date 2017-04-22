package eu.kanade.tachiyomi.ui.manga2

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.RouterPagerAdapter
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.manga2.chapter.ChaptersController
import eu.kanade.tachiyomi.ui.manga2.info.MangaInfoController
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadController
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.manga_controller.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaController : RxController, TabbedController {

    constructor(manga: Manga?) : super(Bundle().apply { putLong(MANGA_EXTRA, manga?.id!!) }) {
        this.manga = manga
        if (manga != null) {
            source = Injekt.get<SourceManager>().get(manga.source)
        }
    }

    constructor(mangaId: Long) : this(
            Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking())

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(MANGA_EXTRA))

    var manga: Manga? = null
        private set

    var source: Source? = null
        private set

    private var adapter: MangaDetailAdapter? = null

    val fromCatalogue = args.getBoolean(FROM_CATALOGUE_EXTRA, false)

    override fun getTitle(): String? {
        return manga?.title
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.manga_controller, container, false)
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (manga == null || source == null) {
            activity?.toast(R.string.manga_not_in_db)
            router.popController(this)
        }
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        if (manga == null || source == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE), 301)
        }

        with(view) {
            adapter = MangaDetailAdapter()
            view_pager.offscreenPageLimit = 3
            view_pager.adapter = adapter

            activity?.tabs?.setupWithViewPager(view_pager)

            if (!fromCatalogue)
                view_pager.currentItem = CHAPTERS_FRAGMENT
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
    }

//    fun setTrackingIcon(visible: Boolean) {
//        val tab = tabs.getTabAt(TRACK_FRAGMENT) ?: return
//        val drawable = if (visible)
//            VectorDrawableCompat.create(resources, R.drawable.ic_done_white_18dp, null)
//        else null
//
//        // I had no choice but to use reflection...
//        val field = tab.javaClass.getDeclaredField("mView").apply { isAccessible = true }
//        val view = field.get(tab) as LinearLayout
//        val textView = view.getChildAt(1) as TextView
//        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
//        textView.compoundDrawablePadding = 4
//    }

    private inner class MangaDetailAdapter : RouterPagerAdapter(this@MangaController) {

        private var tabCount = 2

        private val tabTitles = listOf(
                R.string.manga_detail_tab,
                R.string.manga_chapters_tab,
                R.string.manga_tracking_tab)
                .map { resources!!.getString(it) }

        init {
            val trackManager: TrackManager = Injekt.get()
            if (!fromCatalogue && trackManager.hasLoggedServices())
                tabCount++
        }

        override fun getCount(): Int {
            return tabCount
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller = when (position) {
                    INFO_FRAGMENT -> MangaInfoController()
                    CHAPTERS_FRAGMENT -> ChaptersController()
                    else -> RecentlyReadController()
                }
                router.setRoot(RouterTransaction.with(controller))
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabTitles[position]
        }

    }

    companion object {

        const val FROM_CATALOGUE_EXTRA = "from_catalogue"
        const val MANGA_EXTRA = "manga"
        const val FROM_LAUNCHER_EXTRA = "from_launcher"
        const val INFO_FRAGMENT = 0
        const val CHAPTERS_FRAGMENT = 1
        const val TRACK_FRAGMENT = 2
    }

}
