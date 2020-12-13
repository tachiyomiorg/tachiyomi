package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.system.WebViewClientCompat
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.injectLazy

class WebViewActivity : BaseWebViewActivity() {

    private val sourceManager by injectLazy<SourceManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (bundle == null) {
            val url = intent.extras!!.getString(URL_KEY) ?: return

            var headers = mutableMapOf<String, String>()
            val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource
            if (source != null) {
                headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }.toMutableMap()
                binding.webview.settings.userAgentString = source.headers["User-Agent"]
            }
            headers["X-Requested-With"] = WebViewUtil.REQUESTED_WITH

            supportActionBar?.subtitle = url

            binding.webview.webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                    view.loadUrl(url, headers)
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    invalidateOptionsMenu()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    invalidateOptionsMenu()
                    title = view?.title
                    supportActionBar?.subtitle = url
                    binding.swipeRefresh.isEnabled = true
                    binding.swipeRefresh.isRefreshing = false

                    // Reset to top when page refreshes
                    if (isRefreshing) {
                        view?.scrollTo(0, 0)
                        isRefreshing = false
                    }
                }
            }

            binding.webview.loadUrl(url, headers)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.webview, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val backItem = menu?.findItem(R.id.action_web_back)
        val forwardItem = menu?.findItem(R.id.action_web_forward)
        backItem?.isEnabled = binding.webview.canGoBack()
        forwardItem?.isEnabled = binding.webview.canGoForward()

        val iconTintColor = getResourceColor(R.attr.colorOnPrimary)
        val translucentIconTintColor = ColorUtils.setAlphaComponent(iconTintColor, 127)
        backItem?.icon?.setTint(if (binding.webview.canGoBack()) iconTintColor else translucentIconTintColor)
        forwardItem?.icon?.setTint(if (binding.webview.canGoForward()) iconTintColor else translucentIconTintColor)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_web_back -> binding.webview.goBack()
            R.id.action_web_forward -> binding.webview.goForward()
            R.id.action_web_refresh -> refreshPage()
            R.id.action_web_share -> shareWebpage()
            R.id.action_web_browser -> openInBrowser()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shareWebpage() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, binding.webview.url)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    private fun openInBrowser() {
        openInBrowser(binding.webview.url)
    }

    companion object {
        private const val URL_KEY = "url_key"
        private const val SOURCE_KEY = "source_key"
        private const val TITLE_KEY = "title_key"

        fun newIntent(context: Context, url: String, sourceId: Long? = null, title: String? = null): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(URL_KEY, url)
                putExtra(SOURCE_KEY, sourceId)
                putExtra(TITLE_KEY, title)
            }
        }
    }
}
