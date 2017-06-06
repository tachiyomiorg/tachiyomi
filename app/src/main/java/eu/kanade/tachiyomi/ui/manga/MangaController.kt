package eu.kanade.tachiyomi.ui.manga

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.graphics.drawable.VectorDrawableCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.RouterPagerAdapter
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersController
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoController
import eu.kanade.tachiyomi.ui.manga.track.TrackController
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.manga_controller.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaController : RxController, TabbedController {

    constructor(manga: Manga?, fromCatalogue: Boolean = false) : super(Bundle().apply {
        putLong(MANGA_EXTRA, manga?.id!!)
        putBoolean(FROM_CATALOGUE_EXTRA, fromCatalogue)
    }) {
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

    val chapterCountRelay: BehaviorRelay<Float> = BehaviorRelay.create()

    val mangaFavoriteRelay: PublishRelay<Boolean> = PublishRelay.create()

    override fun getTitle(): String? {
        return manga?.title
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.manga_controller, container, false)
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

            if (!fromCatalogue)
                view_pager.currentItem = CHAPTERS_CONTROLLER
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            activity?.tabs?.setupWithViewPager(view?.view_pager)
        }
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (manga == null || source == null) {
            activity?.toast(R.string.manga_not_in_db)
            router.popController(this)
        }
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
    }

    override fun cleanupTabs(tabs: TabLayout) {
        setTrackingIcon(false)
    }

    fun setTrackingIcon(visible: Boolean) {
        val tab = activity?.tabs?.getTabAt(TRACK_CONTROLLER) ?: return
        val drawable = if (visible)
            VectorDrawableCompat.create(resources!!, R.drawable.ic_done_white_18dp, null)
        else null

        // I had no choice but to use reflection...
        val view = tabField.get(tab) as LinearLayout
        val textView = view.getChildAt(1) as TextView
        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        textView.compoundDrawablePadding = if (visible) 4 else 0
    }

    private inner class MangaDetailAdapter : RouterPagerAdapter(this@MangaController) {

        private val tabCount = if (Injekt.get<TrackManager>().hasLoggedServices()) 3 else 2

        private val tabTitles = listOf(
                R.string.manga_detail_tab,
                R.string.manga_chapters_tab,
                R.string.manga_tracking_tab)
                .map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabCount
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val controller = when (position) {
                    INFO_CONTROLLER -> MangaInfoController()
                    CHAPTERS_CONTROLLER -> ChaptersController()
                    TRACK_CONTROLLER -> TrackController()
                    else -> error("Wrong position $position")
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

        const val INFO_CONTROLLER = 0
        const val CHAPTERS_CONTROLLER = 1
        const val TRACK_CONTROLLER = 2

        private val tabField = TabLayout.Tab::class.java.getDeclaredField("mView")
                .apply { isAccessible = true }
    }

}
