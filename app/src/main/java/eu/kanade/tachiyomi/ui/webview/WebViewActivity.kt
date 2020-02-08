package eu.kanade.tachiyomi.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.*
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.webview_activity.*
import uy.kohesive.injekt.injectLazy

class WebViewActivity : BaseActivity() {

    private val sourceManager by injectLazy<SourceManager>()

    private var bundle: Bundle? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity)

        // Manually override status bar color since it's normally transparent with the app themes
        // This is needed to hide the app bar when it scrolls up
        window.statusBarColor = getResourceColor(R.attr.colorPrimaryDark)

        title = intent.extras?.getString(TITLE_KEY)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            super.onBackPressed()
        }

        swipe_refresh.isEnabled = false
        swipe_refresh.setOnRefreshListener {
            refreshPage()
        }

        if (bundle == null) {
            val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource ?: return
            val url = intent.extras!!.getString(URL_KEY) ?: return
            val headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

            webview.checkVersion()

            webview.settings.javaScriptEnabled = true
            webview.settings.userAgentString = source.headers["User-Agent"]

            webview.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress_bar.visible()
                    progress_bar.progress = newProgress
                    if (newProgress == 100) {
                        progress_bar.invisible()
                    }
                    super.onProgressChanged(view, newProgress)
                }
            }

            webview.webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    invalidateOptionsMenu()
                    title = view?.title
                    swipe_refresh.isEnabled = true
                    swipe_refresh?.isRefreshing = false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    invalidateOptionsMenu()
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)

                    // Reset to top when page refreshes
                    nested_view.scrollTo(0, 0)
                }
            }

            webview.loadUrl(url, headers)
        } else {
            webview.restoreState(bundle)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.webview, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val backItem = toolbar.menu.findItem(R.id.action_web_back)
        val forwardItem = toolbar.menu.findItem(R.id.action_web_forward)
        backItem?.isEnabled = webview.canGoBack()
        forwardItem?.isEnabled = webview.canGoForward()

        val translucentWhite = ColorUtils.setAlphaComponent(Color.WHITE, 127)
        backItem.icon?.setTint(if (webview.canGoBack()) Color.WHITE else translucentWhite)
        forwardItem?.icon?.setTint(if (webview.canGoForward()) Color.WHITE else translucentWhite)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onBackPressed() {
        if (webview.canGoBack()) webview.goBack()
        else super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_web_back -> webview.goBack()
            R.id.action_web_forward -> webview.goForward()
            R.id.action_web_refresh -> refreshPage()
            R.id.action_web_share -> shareWebpage()
            R.id.action_web_browser -> openInBrowser()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshPage() {
        swipe_refresh.isRefreshing = true
        webview.reload()
    }

    private fun shareWebpage() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, webview.url)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    private fun openInBrowser() {
        openInBrowser(webview.url)
    }

    companion object {
        private const val SOURCE_KEY = "source_key"
        private const val URL_KEY = "url_key"
        private const val TITLE_KEY = "title_key"

        fun newIntent(context: Context, sourceId: Long, url: String, title: String?): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(SOURCE_KEY, sourceId)
            intent.putExtra(URL_KEY, url)
            intent.putExtra(TITLE_KEY, title)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }
    }
}
