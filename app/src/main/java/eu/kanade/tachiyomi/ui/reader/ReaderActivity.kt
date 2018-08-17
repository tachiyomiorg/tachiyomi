package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.reader.ReaderPresenter.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.reader.ReaderPresenter.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.ReaderPresenter.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.*
import eu.kanade.tachiyomi.widget.SimpleAnimationListener
import eu.kanade.tachiyomi.widget.SimpleSeekBarListener
import kotlinx.android.synthetic.main.reader_activity.*
import me.zhanghai.android.systemuihelper.SystemUiHelper
import nucleus.factory.RequiresPresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit

@RequiresPresenter(ReaderPresenter::class)
class ReaderActivity : BaseRxActivity<ReaderPresenter>() {

    private val preferences by injectLazy<PreferencesHelper>()

    var viewer: BaseViewer? = null
        private set

    val maxBitmapSize by lazy { GLUtil.getMaxTextureSize() }

    var menuVisible = false
        private set

    private var systemUi: SystemUiHelper? = null

    private var config: ReaderConfig? = null

    @Suppress("DEPRECATION")
    private var progressDialog: ProgressDialog? = null

    companion object {
        @Suppress("unused")
        const val LEFT_TO_RIGHT = 1
        const val RIGHT_TO_LEFT = 2
        const val VERTICAL = 3
        const val WEBTOON = 4

        fun newIntent(context: Context, manga: Manga, chapter: Chapter): Intent {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra("manga", manga)
            intent.putExtra("chapter", chapter.id)
            return intent
        }
    }

    /**
     * Lifecycle methods
     */

    override fun onCreate(savedState: Bundle?) {
        setTheme(when (preferences.readerTheme().getOrDefault()) {
            0 -> R.style.Theme_Reader_Light
            else -> R.style.Theme_Reader
        })
        super.onCreate(savedState)
        setContentView(R.layout.reader_activity)

        if (presenter.needsInit()) {
            val manga = intent.extras.getSerializable("manga") as? Manga
            val chapter = intent.extras.getLong("chapter", -1)

            if (manga == null || chapter == -1L) {
                finish()
                return
            }

            presenter.init(manga, chapter)
        }

        if (savedState != null) {
            menuVisible = savedState.getBoolean(::menuVisible.name)
        }

        config = ReaderConfig()
        initializeMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewer?.destroy()
        config?.destroy()
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(::menuVisible.name, menuVisible)
        if (!isChangingConfigurations) {
            presenter.saveCurrentProgress()
        }
        super.onSaveInstanceState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(menuVisible, animate = false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> ReaderSettingsSheet(this).show()
            R.id.action_custom_filter -> ReaderColorFilterSheet(this).show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        presenter.updateTrackLastChapterRead()
        super.onBackPressed()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewer?.handleKeyEvent(event) ?: false
        return handled || super.dispatchKeyEvent(event)
    }

    private fun initializeMenu() {
        // Set toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Init listeners on bottom menu
        page_seekbar.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                if (viewer != null && fromUser) {
                    moveToPageIndex(value)
                }
            }
        })
        left_chapter.setOnClickListener {
            if (viewer != null) {
                if (viewer is R2LPagerViewer)
                    moveToNextChapter()
                else
                    moveToPrevChapter()
            }
        }
        right_chapter.setOnClickListener {
            if (viewer != null) {
                if (viewer is R2LPagerViewer)
                    moveToPrevChapter()
                else
                    moveToNextChapter()
            }
        }

        // Set initial visibility
        setMenuVisibility(menuVisible)
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
                            window.addFlags(
                                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
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
     * Methods called from presenter or this activity.
     */

    fun setManga(manga: Manga) {
        val prevViewer = viewer
        val newViewer = when (presenter.getMangaViewer()) {
            RIGHT_TO_LEFT -> R2LPagerViewer(this)
            VERTICAL -> VerticalPagerViewer(this)
            WEBTOON -> WebtoonViewer(this)
            else -> L2RPagerViewer(this)
        }

        if (prevViewer != null) {
            prevViewer.destroy()
            viewer_container.removeAllViews()
        }
        viewer = newViewer
        viewer_container.addView(newViewer.getView())

        toolbar.title = manga.title

        page_seekbar.isReversed = newViewer is R2LPagerViewer

        please_wait.visible()
        please_wait.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_long))
    }

    fun setChapters(viewerChapters: ViewerChapters) {
        please_wait.gone()
        viewer?.setChapters(viewerChapters)
        toolbar.subtitle = viewerChapters.currChapter.chapter.name
    }

    fun setInitialChapterError(error: Throwable) {
        Timber.e(error)
        finish()
        toast(error.message)
    }

    @Suppress("DEPRECATION")
    fun setProgressBar(show: Boolean) {
        progressDialog?.dismiss()
        progressDialog = if (show) {
            ProgressDialog.show(this, null, getString(R.string.loading), true)
        } else {
            null
        }
    }

    fun moveToPageIndex(index: Int) {
        val currentChapter = presenter.getCurrentChapter() ?: return
        val page = currentChapter.pages?.getOrNull(index) ?: return
        moveToPage(page)
    }

    fun moveToPage(page: ReaderPage) {
        viewer?.moveToPage(page)
    }

    private fun moveToNextChapter() {
        presenter.loadNextChapter()
    }

    private fun moveToPrevChapter() {
        presenter.loadPreviousChapter()
    }

    /**
     * Methods called from the viewer.
     */

    @SuppressLint("SetTextI18n")
    fun onPageSelected(page: ReaderPage) {
        presenter.onPageSelected(page)
        val pages = page.chapter.pages ?: return

        // Set bottom page number
        page_number.text = "${page.number}/${pages.size}"

        // Set seekbar page number
        if (viewer !is R2LPagerViewer) {
            left_page_text.text = "${page.number}"
            right_page_text.text = "${pages.size}"
        } else {
            right_page_text.text = "${page.number}"
            left_page_text.text = "${pages.size}"
        }

        // Set seekbar progress
        page_seekbar.max = pages.lastIndex
        page_seekbar.progress = page.index
    }

    fun onPageLongTap(page: ReaderPage) {
        ReaderPageSheet(this, page).show()
    }

    fun requestPreloadNextChapter() {
        presenter.preloadNextChapter()
    }

    fun requestPreloadPreviousChapter() {
        presenter.preloadPreviousChapter()
    }

    fun toggleMenu() {
        setMenuVisibility(!menuVisible)
    }

    /**
     * Reader page sheet
     */

    fun shareImage(page: ReaderPage) {
        presenter.shareImage(page)
    }

    fun onShareImageResult(file: File) {
        val stream = file.getUriCompat(this)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, stream)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/*"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    fun saveImage(page: ReaderPage) {
        presenter.saveImage(page)
    }

    fun onSaveImageResult(result: ReaderPresenter.SaveImageResult) {
        when (result) {
            is ReaderPresenter.SaveImageResult.Success -> {
                toast(R.string.picture_saved)
            }
            is ReaderPresenter.SaveImageResult.Error -> {
                Timber.e(result.error)
            }
        }
    }

    fun setAsCover(page: ReaderPage) {
        presenter.setAsCover(page)
    }

    fun onSetAsCoverResult(result: ReaderPresenter.SetAsCoverResult) {
        toast(when (result) {
            Success -> R.string.cover_updated
            AddToLibraryFirst -> R.string.notification_first_add_to_library
            Error -> R.string.notification_cover_update_failed
        })
    }

    /**
     * Reader config
     */

    private inner class ReaderConfig {

        private val subscriptions = CompositeSubscription()

        private var customBrightnessSubscription: Subscription? = null

        private var customFilterColorSubscription: Subscription? = null

        init {
            val sharedRotation = preferences.rotation().asObservable().share()
            val initialRotation = sharedRotation.take(1)
            val rotationUpdates = sharedRotation.skip(1)
                .delay(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())

            subscriptions += Observable.merge(initialRotation, rotationUpdates)
                .subscribe { setOrientation(it) }

            subscriptions += preferences.readerTheme().asObservable()
                .skip(1) // We only care about updates
                .subscribe { recreate() }

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
        }

        fun destroy() {
            subscriptions.unsubscribe()
            customBrightnessSubscription = null
            customFilterColorSubscription = null
        }

        private fun setOrientation(orientation: Int) {
            val newOrientation = when (orientation) {
                // Lock in current orientation
                2 -> {
                    val currentOrientation = resources.configuration.orientation
                    if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                }
                // Lock in portrait
                3 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                // Lock in landscape
                4 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                // Rotation free
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

            if (newOrientation != requestedOrientation) {
                requestedOrientation = newOrientation
            }
        }

        private fun setPageNumberVisibility(visible: Boolean) {
            page_number.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }

        private fun setFullscreen(enabled: Boolean) {
            systemUi = if (enabled) {
                val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    SystemUiHelper.LEVEL_IMMERSIVE
                } else {
                    SystemUiHelper.LEVEL_HIDE_STATUS_BAR
                }
                val flags = SystemUiHelper.FLAG_IMMERSIVE_STICKY or
                        SystemUiHelper.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES

                SystemUiHelper(this@ReaderActivity, level, flags)
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

    }

}
