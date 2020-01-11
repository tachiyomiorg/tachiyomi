package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.WebViewClientCompat
import kotlinx.android.synthetic.main.webview_activity.toolbar
import kotlinx.android.synthetic.main.webview_activity.webview
import uy.kohesive.injekt.injectLazy


class WebViewActivity : BaseActivity() {

    private val sourceManager by injectLazy<SourceManager>()

    private var bundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity)

        title = intent.extras?.getString(TITLE_KEY)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            super.onBackPressed()
        }

        if (bundle == null) {
            val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource ?: return
            val url = intent.extras!!.getString(URL_KEY) ?: return
            val headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

            webview.webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    invalidateOptionsMenu()
                    title = view?.title
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    invalidateOptionsMenu()
                }
            }
            webview.settings.javaScriptEnabled = true
            webview.settings.userAgentString = source.headers["User-Agent"]
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
            R.id.action_web_refresh -> webview.reload()
        }
        return super.onOptionsItemSelected(item)
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
