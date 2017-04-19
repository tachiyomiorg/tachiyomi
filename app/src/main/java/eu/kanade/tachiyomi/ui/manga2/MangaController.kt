package eu.kanade.tachiyomi.ui.manga2

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.manga2.info.MangaInfoController
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadController
import eu.kanade.tachiyomi.util.SharedData
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.manga_controller.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaController(bundle: Bundle? = null) : RxController(bundle),
        TabbedController {

    private val trackManager: TrackManager = Injekt.get()

    lateinit var manga: Manga

    private var adapter: MangaDetailAdapter? = null

    val fromCatalogue: Boolean = bundle?.getBoolean(FROM_CATALOGUE_EXTRA) ?: false

    constructor(manga: Manga?) : this(Bundle().apply {
        putLong(MANGA_EXTRA, manga?.id!!)
    }) {
        this.manga = manga!!
    }

    constructor(mangaId: Long) : this(
            Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking())

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    companion object {

        const val FROM_CATALOGUE_EXTRA = "from_catalogue"
        const val MANGA_EXTRA = "manga"
        const val FROM_LAUNCHER_EXTRA = "from_launcher"
        const val INFO_FRAGMENT = 0
        const val CHAPTERS_FRAGMENT = 1
        const val TRACK_FRAGMENT = 2

        fun newIntent(context: Context, manga: Manga, fromCatalogue: Boolean = false): Intent {
            SharedData.put(MangaEvent(manga))
            return Intent(context, MangaController::class.java).apply {
                putExtra(FROM_CATALOGUE_EXTRA, fromCatalogue)
                putExtra(MANGA_EXTRA, manga.id)
            }
        }
    }

    override fun getTitle(): String? {
        return manga.title
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.manga_controller, container, false)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

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
//        activity?.tabs?.setupWithViewPager(null)
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
            if (!fromCatalogue && trackManager.hasLoggedServices())
                tabCount++
        }

        override fun getCount(): Int {
            return tabCount
        }

        override fun configureRouter(router: Router, position: Int) {
            val controller = when (position) {
                1 -> MangaInfoController()
                else -> RecentlyReadController()
            }
            router.setRoot(RouterTransaction.with(controller))
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabTitles[position]
        }

    }

}
