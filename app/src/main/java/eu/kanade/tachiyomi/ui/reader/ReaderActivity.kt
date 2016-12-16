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
import eu.kanade.tachiyomi.data.source.model.Page
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
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

@RequiresPresenter(ReaderPresenter::class)
class ReaderActivity : BaseRxActivity<ReaderPresenter>() {

    companion object {
        @Suppress("unused")
        const val LEFT_TO_RIGHT = 1
        const val RIGHT_TO_LEFT = 2
        const val VERTICAL = 3
        const val WEBTOON = 4

        const val WHITE_THEME = 0
        const val BLACK_THEME = 1

        const val MENU_VISIBLE = "menu_visible"

        fun newIntent(context: Context, manga: Manga, chapter: Chapter): Intent {
            SharedData.put(ReaderEvent(manga, chapter))
            return Intent(context, ReaderActivity::class.java)
        }
    }

    private var viewer: BaseReader? = null

    val subscriptions by lazy { CompositeSubscription() }

    private var customBrightnessSubscription: Subscription? = null

    private var customFilterColorSubscription: Subscription? = null

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
            R.id.action_custom_filter -> ReaderCustomFilterDialog().show(supportFragmentManager, "filter")
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(MENU_VISIBLE, menuVisible)
        super.onSaveInstanceState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(menuVisible, animate = false)
        }
    }

    override fun onBackPressed() {
        val chapterToUpdate = presenter.getTrackChapterToUpdate()

        if (chapterToUpdate > 0) {
            if (preferences.askUpdateTrack()) {
                MaterialDialog.Builder(this)
                        .content(getString(R.string.confirm_update_manga_sync, chapterToUpdate))
                        .positiveText(android.R.string.yes)
                        .negativeText(android.R.string.no)
                        .onPositive { dialog, which -> presenter.updateTrackLastChapterRead() }
                        .onAny { dialog1, which1 -> super.onBackPressed() }
                        .show()
            } else {
                presenter.updateTrackLastChapterRead()
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
                            viewer?.moveDown()
                        }
                        return true
                    }
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (volumeKeysEnabled) {
                        if (event.action == KeyEvent.ACTION_UP) {
                            viewer?.moveUp()
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
                KeyEvent.KEYCODE_DPAD_RIGHT -> viewer?.moveRight()
                KeyEvent.KEYCODE_DPAD_LEFT -> viewer?.moveLeft()
                KeyEvent.KEYCODE_DPAD_DOWN -> viewer?.moveDown()
                KeyEvent.KEYCODE_DPAD_UP -> viewer?.moveUp()
                KeyEvent.KEYCODE_MENU -> toggleMenu()
                else -> return super.onKeyUp(keyCode, event)
            }
        }
        return true
    }

    fun onChapterError(error: Throwable) {
        Timber.e(error)
        finish()
        toast(error.message)
    }

    fun onLongClick(page: Page) {
        MaterialDialog.Builder(this)
                .title(getString(R.string.options))
                .items(R.array.reader_image_options)
                .itemsIds(R.array.reader_image_options_values)
                .itemsCallback { materialDialog, view, i, charSequence ->
                    when (i) {
                        0 -> setImageAsCover(page)
                        1 -> shareImage(page)
                        2 -> presenter.savePage(page)
                    }
                }.show()
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
        setActiveChapter(chapter, activePage.index)
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
        var fragment = supportFragmentManager.findFragmentByTag(manga.viewer.toString()) as? BaseReader
        if (fragment == null) {
            // Create a new viewer
            fragment = when (mangaViewer) {
                RIGHT_TO_LEFT -> RightToLeftReader()
                VERTICAL -> VerticalReader()
                WEBTOON -> WebtoonReader()
                else -> LeftToRightReader()
            }

            supportFragmentManager.beginTransaction().replace(R.id.reader, fragment, manga.viewer.toString()).commit()
        }
        return fragment
    }

    fun onPageChanged(page: Page) {
        presenter.onPageChanged(page)

        val pageNumber = page.number
        val pageCount = page.chapter.pages!!.size
        page_number.text = "$pageNumber/$pageCount"
        if (page_seekbar.rotation != 180f) {
            left_page_text.text = "$pageNumber"
        } else {
            right_page_text.text = "$pageNumber"
        }
        page_seekbar.progress = page.index
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
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    gotoPageInCurrentChapter(value)
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

        subscriptions += preferences.colorFilter().asObservable()
                .subscribe { setColorFilter(it) }

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
                    .sample(100, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .subscribe { setCustomBrightnessValue(it) }

            subscriptions.add(customBrightnessSubscription)
        } else {
            customBrightnessSubscription?.let { subscriptions.remove(it) }
            setCustomBrightnessValue(0)
        }
    }

    private fun setColorFilter(enabled: Boolean) {
        if (enabled) {
            customFilterColorSubscription = preferences.colorFilterValue().asObservable()
                    .sample(100, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .subscribe { setColorFilterValue(it) }

            subscriptions.add(customFilterColorSubscription)
        } else {
            customFilterColorSubscription?.let { subscriptions.remove(it) }
            color_overlay.visibility = View.GONE
        }
    }

    /**
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness and hides the overlay.
     */
    private fun setCustomBrightnessValue(value: Int) {
        // Calculate and set reader brightness.
        val readerBrightness = if (value > 0) {
            value / 100f
        } else if (value < 0) {
            0.01f
        } else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        window.attributes = window.attributes.apply { screenBrightness = readerBrightness }

        // Set black overlay visibility.
        if (value < 0) {
            brightness_overlay.visibility = View.VISIBLE
            val alpha = (Math.abs(value) * 2.56).toInt()
            brightness_overlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
        } else {
            brightness_overlay.visibility = View.GONE
        }
    }

    private fun setColorFilterValue(value: Int) {
        color_overlay.visibility = View.VISIBLE
        color_overlay.setBackgroundColor(value)
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

    private fun setMenuVisibility(visible: Boolean, animate: Boolean = true) {
        menuVisible = visible
        if (visible) {
            systemUi?.show()
            reader_menu.visibility = View.VISIBLE

            if (animate) {
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
            }
        } else {
            systemUi?.hide()

            if (animate) {
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

    /**
     * Start a share intent that lets user share image
     *
     * @param page page object containing image information.
     */
    private fun shareImage(page: Page) {
        if (page.status != Page.READY)
            return

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, page.uri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/*"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    /**
     * Sets the given page as the cover of the manga.
     *
     * @param page the page containing the image to set as cover.
     */
    private fun setImageAsCover(page: Page) {
        if (page.status != Page.READY)
            return

        MaterialDialog.Builder(this)
                .content(getString(R.string.confirm_set_image_as_cover))
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive { dialog, which -> presenter.setImageAsCover(page) }
                .show()

    }

}
