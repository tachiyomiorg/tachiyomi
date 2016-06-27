package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.*
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.reader.viewer.base.BaseReader
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.LeftToRightReader
import eu.kanade.tachiyomi.ui.reader.viewer.pager.horizontal.RightToLeftReader
import eu.kanade.tachiyomi.ui.reader.viewer.pager.vertical.VerticalReader
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonReader
import eu.kanade.tachiyomi.util.GLUtil
import eu.kanade.tachiyomi.util.SharedData
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.SimpleAnimationListener
import eu.kanade.tachiyomi.widget.SimpleSeekBarListener
import kotlinx.android.synthetic.main.activity_reader.*
import me.zhanghai.android.systemuihelper.SystemUiHelper
import me.zhanghai.android.systemuihelper.SystemUiHelper.*
import nucleus.factory.RequiresPresenter
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat

@RequiresPresenter(ReaderPresenter::class)
class ReaderActivity : BaseRxActivity<ReaderPresenter>() {

    companion object {
        @Suppress("unused")
        const val LEFT_TO_RIGHT = 1
        const val RIGHT_TO_LEFT = 2
        const val VERTICAL = 3
        const val WEBTOON = 4

        const val BLACK_THEME = 1

        const val MENU_VISIBLE = "menu_visible"

        fun newIntent(context: Context, manga: Manga, chapter: Chapter): Intent {
            SharedData.put(ReaderEvent(manga, chapter))
            return Intent(context, ReaderActivity::class.java)
        }
    }

    private var viewer: BaseReader? = null

    lateinit var subscriptions: CompositeSubscription
        private set

    private var customBrightnessSubscription: Subscription? = null

    var readerTheme: Int = 0
        private set

    var maxBitmapSize: Int = 0
        private set

    private val decimalFormat = DecimalFormat("#.###")

    private val volumeKeysEnabled by lazy { preferences.readWithVolumeKeys().getOrDefault() }

    val preferences by injectLazy<PreferencesHelper>()

    private var systemUi: SystemUiHelper? = null

    private var menuVisible = false

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setContentView(R.layout.activity_reader)

        if (savedState == null && SharedData.get(ReaderEvent::class.java) == null) {
            finish()
            return
        }

        setupToolbar(toolbar)
        subscriptions = CompositeSubscription()

        initializeSettings()
        initializeBottomMenu()

        if (savedState != null) {
            menuVisible = savedState.getBoolean(MENU_VISIBLE)
        }

        setMenuVisibility(menuVisible)

        maxBitmapSize = GLUtil.getMaxTextureSize()

        left_chapter.setOnClickListener {
            if (viewer != null) {
                if (viewer is RightToLeftReader)
                    requestNextChapter()
                else
                    requestPreviousChapter()
            }
        }
        right_chapter.setOnClickListener {
            if (viewer != null) {
                if (viewer is RightToLeftReader)
                    requestPreviousChapter()
                else
                    requestNextChapter()
            }
        }
    }

    override fun onDestroy() {
        subscriptions.unsubscribe()
        viewer = null
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> ReaderSettingsDialog().show(supportFragmentManager, "settings")
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(MENU_VISIBLE, menuVisible)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        val chapterToUpdate = presenter.getMangaSyncChapterToUpdate()

        if (chapterToUpdate > 0) {
            if (preferences.askUpdateMangaSync()) {
                MaterialDialog.Builder(this)
                        .content(getString(R.string.confirm_update_manga_sync, chapterToUpdate))
                        .positiveText(android.R.string.yes)
                        .negativeText(android.R.string.no)
                        .onPositive { dialog, which -> presenter.updateMangaSyncLastChapterRead() }
                        .onAny { dialog1, which1 -> super.onBackPressed() }
                        .show()
            } else {
                presenter.updateMangaSyncLastChapterRead()
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isFinishing) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (volumeKeysEnabled) {
                        if (event.action == KeyEvent.ACTION_UP) {
                            viewer?.moveToNext()
                        }
                        return true
                    }
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (volumeKeysEnabled) {
                        if (event.action == KeyEvent.ACTION_UP) {
                            viewer?.moveToPrevious()
                        }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isFinishing) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> viewer?.moveToNext()
                KeyEvent.KEYCODE_DPAD_LEFT -> viewer?.moveToPrevious()
                KeyEvent.KEYCODE_MENU -> toggleMenu()
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    fun onChapterError(error: Throwable) {
        Timber.e(error, error.message)
        finish()
        toast(error.message)
    }

    fun onChapterAppendError() {
        // Ignore
    }

    /**
     * Called from the presenter at startup, allowing to prepare the selected reader.
     */
    fun onMangaOpen(manga: Manga) {
        if (viewer == null) {
            viewer = getOrCreateViewer(manga)
        }
        if (viewer is RightToLeftReader && page_seekbar.rotation != 180f) {
            // Invert the seekbar for the right to left reader
            page_seekbar.rotation = 180f
        }
        setToolbarTitle(manga.title)
        please_wait.visibility = View.VISIBLE
        please_wait.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_long))
    }

    fun onChapterReady(chapter: ReaderChapter) {
        please_wait.visibility = View.GONE
        val pages = chapter.pages ?: run { onChapterError(Exception("Null pages")); return }
        val activePage = pages.getOrElse(chapter.requestedPage) { pages.first() }

        viewer?.onPageListReady(chapter, activePage)
        setActiveChapter(chapter, activePage.pageNumber)
    }

    fun onEnterChapter(chapter: ReaderChapter, currentPage: Int) {
        val activePage = if (currentPage == -1) chapter.pages!!.lastIndex else currentPage
        presenter.setActiveChapter(chapter)
        setActiveChapter(chapter, activePage)
    }

    fun setActiveChapter(chapter: ReaderChapter, currentPage: Int) {
        val numPages = chapter.pages!!.size
        if (page_seekbar.rotation != 180f) {
            right_page_text.text = "$numPages"
            left_page_text.text = "${currentPage + 1}"
        } else {
            left_page_text.text = "$numPages"
            right_page_text.text = "${currentPage + 1}"
        }
        page_seekbar.max = numPages - 1
        page_seekbar.progress = currentPage

        setToolbarSubtitle(if (chapter.isRecognizedNumber)
            getString(R.string.chapter_subtitle, decimalFormat.format(chapter.chapter_number.toDouble()))
        else
            chapter.name)
    }

    fun onAppendChapter(chapter: ReaderChapter) {
        viewer?.onPageListAppendReady(chapter)
    }

    fun onAdjacentChapters(previous: Chapter?, next: Chapter?) {
        val isInverted = viewer is RightToLeftReader

        // Chapters are inverted for the right to left reader
        val hasRightChapter = (if (isInverted) previous else next) != null
        val hasLeftChapter = (if (isInverted) next else previous) != null

        right_chapter.isEnabled = hasRightChapter
        right_chapter.alpha = if (hasRightChapter) 1f else 0.4f

        left_chapter.isEnabled = hasLeftChapter
        left_chapter.alpha = if (hasLeftChapter) 1f else 0.4f
    }

    private fun getOrCreateViewer(manga: Manga): BaseReader {
        val mangaViewer = if (manga.viewer == 0) preferences.defaultViewer() else manga.viewer

        // Try to reuse the viewer using its tag
        var fragment: BaseReader? = supportFragmentManager.findFragmentByTag(manga.viewer.toString()) as? BaseReader
        if (fragment == null) {
            // Create a new viewer
            when (mangaViewer) {
                RIGHT_TO_LEFT -> fragment = RightToLeftReader()
                VERTICAL -> fragment = VerticalReader()
                WEBTOON -> fragment = WebtoonReader()
                else -> fragment = LeftToRightReader()
            }

            supportFragmentManager.beginTransaction().replace(R.id.reader, fragment, manga.viewer.toString()).commit()
        }
        return fragment
    }

    fun onPageChanged(currentPageIndex: Int, totalPages: Int) {
        val page = currentPageIndex + 1
        page_number.text = "$page/$totalPages"
        if (page_seekbar.rotation != 180f) {
            left_page_text.text = "$page"
        } else {
            right_page_text.text = "$page"
        }
        page_seekbar.progress = currentPageIndex
    }

    fun gotoPageInCurrentChapter(pageIndex: Int) {
        viewer?.let {
            val activePage = it.getActivePage()
            if (activePage != null) {
                val requestedPage = activePage.chapter.pages!![pageIndex]
                it.setActivePage(requestedPage)
            }

        }
    }

    fun toggleMenu() {
        setMenuVisibility(!menuVisible)
    }

    fun requestNextChapter() {
        if (!presenter.loadNextChapter()) {
            toast(R.string.no_next_chapter)
        }
    }

    fun requestPreviousChapter() {
        if (!presenter.loadPreviousChapter()) {
            toast(R.string.no_previous_chapter)
        }
    }

    private fun initializeBottomMenu() {
        // Intercept all events in this layout
        reader_menu_bottom.setOnTouchListener { v, event -> true }

        page_seekbar.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    gotoPageInCurrentChapter(progress)
                }
            }
        })
    }

    private fun initializeSettings() {
        subscriptions += preferences.rotation().asObservable()
                .subscribe { setRotation(it) }

        subscriptions += preferences.showPageNumber().asObservable()
                .subscribe { setPageNumberVisibility(it) }

        subscriptions += preferences.fullscreen().asObservable()
                .subscribe { setFullscreen(it) }

        subscriptions += preferences.keepScreenOn().asObservable()
                .subscribe { setKeepScreenOn(it) }

        subscriptions += preferences.customBrightness().asObservable()
                .subscribe { setCustomBrightness(it) }

        subscriptions += preferences.readerTheme().asObservable()
                .distinctUntilChanged()
                .subscribe { applyTheme(it) }
    }

    private fun setRotation(rotation: Int) {
        when (rotation) {
            // Rotation free
            1 -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // Lock in current rotation
            2 -> {
                val currentOrientation = resources.configuration.orientation
                setRotation(if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) 3 else 4)
            }
            // Lock in portrait
            3 -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            // Lock in landscape
            4 -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun setPageNumberVisibility(visible: Boolean) {
        page_number.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    private fun setFullscreen(enabled: Boolean) {
        systemUi = if (enabled) {
            val level = if (Build.VERSION.SDK_INT >= KITKAT) LEVEL_IMMERSIVE else LEVEL_HIDE_STATUS_BAR
            val flags = FLAG_IMMERSIVE_STICKY or FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES
            SystemUiHelper(this, level, flags)
        } else {
            null
        }
    }

    private fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setCustomBrightness(enabled: Boolean) {
        if (enabled) {
            customBrightnessSubscription = preferences.customBrightnessValue().asObservable()
                    .map { Math.max(0.01f, it) }
                    .subscribe { setCustomBrightnessValue(it) }

            subscriptions.add(customBrightnessSubscription)
        } else {
            if (customBrightnessSubscription != null) {
                subscriptions.remove(customBrightnessSubscription)
            }
            setCustomBrightnessValue(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    private fun setCustomBrightnessValue(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }

    private fun applyTheme(theme: Int) {
        readerTheme = theme
        val rootView = window.decorView.rootView
        if (theme == BLACK_THEME) {
            rootView.setBackgroundColor(Color.BLACK)
            page_number.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimaryDark))
            page_number.setBackgroundColor(ContextCompat.getColor(this, R.color.pageNumberBackgroundDark))
        } else {
            rootView.setBackgroundColor(Color.WHITE)
            page_number.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimaryLight))
            page_number.setBackgroundColor(ContextCompat.getColor(this, R.color.pageNumberBackgroundLight))
        }
    }

    private fun setMenuVisibility(visible: Boolean) {
        menuVisible = visible
        if (visible) {
            systemUi?.show()
            reader_menu.visibility = View.VISIBLE

            val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_top)
            toolbarAnimation.setAnimationListener(object : SimpleAnimationListener() {
                override fun onAnimationStart(animation: Animation) {
                    // Fix status bar being translucent the first time it's opened.
                    if (Build.VERSION.SDK_INT >= 21) {
                        window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    }
                }
            })
            toolbar.startAnimation(toolbarAnimation)

            val bottomMenuAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom)
            reader_menu_bottom.startAnimation(bottomMenuAnimation)
        } else {
            systemUi?.hide()
            val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_top)
            toolbarAnimation.setAnimationListener(object : SimpleAnimationListener() {
                override fun onAnimationEnd(animation: Animation) {
                    reader_menu.visibility = View.GONE
                }
            })
            toolbar.startAnimation(toolbarAnimation)

            val bottomMenuAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_bottom)
            reader_menu_bottom.startAnimation(bottomMenuAnimation)
        }
    }

}
