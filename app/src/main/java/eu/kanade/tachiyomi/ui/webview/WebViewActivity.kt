package eu.kanade.tachiyomi.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.graphics.ColorUtils
import androidx.webkit.WebViewClientCompat
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.WebviewActivityBinding
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.main.ForceCloseActivity
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.navigationClicks
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import uy.kohesive.injekt.injectLazy

class WebViewActivity : BaseActivity<WebviewActivityBinding>() {

    private val sourceManager by injectLazy<SourceManager>()

    private var bundle: Bundle? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = WebviewActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            // Potentially throws errors like "Error inflating class android.webkit.WebView"
            ForceCloseActivity.closeApp(this)
        }

        title = intent.extras?.getString(TITLE_KEY)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.navigationClicks()
            .onEach { super.onBackPressed() }
            .launchIn(scope)

        binding.swipeRefresh.isEnabled = false
        binding.swipeRefresh.refreshes()
            .onEach { refreshPage() }
            .launchIn(scope)

        if (bundle == null) {
            val url = intent.extras!!.getString(URL_KEY) ?: return
            var headers = emptyMap<String, String>()

            val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource
            if (source != null) {
                headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
                binding.webview.settings.userAgentString = source.headers["User-Agent"]
            }

            supportActionBar?.subtitle = url

            // Debug mode (chrome://inspect/#devices)
            if (BuildConfig.DEBUG && 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            binding.webview.settings.javaScriptEnabled = true
            binding.webview.settings.domStorageEnabled = true

            binding.webview.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.visible()
                    binding.progressBar.progress = newProgress
                    if (newProgress == 100) {
                        binding.progressBar.invisible()
                    }
                    super.onProgressChanged(view, newProgress)
                }
            }

            binding.webview.webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    view.loadUrl(request.url.toString())
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    invalidateOptionsMenu()
                    title = view?.title
                    supportActionBar?.subtitle = url
                    binding.swipeRefresh.isEnabled = true
                    binding.swipeRefresh.isRefreshing = false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    invalidateOptionsMenu()
                }

                override fun onPageCommitVisible(view: WebView, url: String) {
                    super.onPageCommitVisible(view, url)

                    // Reset to top when page refreshes
                    binding.nestedView.scrollTo(0, 0)
                }
            }

            binding.webview.loadUrl(url, headers)
        } else {
            binding.webview.restoreState(bundle)
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

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) binding.webview.goBack()
        else super.onBackPressed()
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

    private fun refreshPage() {
        binding.swipeRefresh.isRefreshing = true
        binding.webview.reload()
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
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(URL_KEY, url)
            intent.putExtra(SOURCE_KEY, sourceId)
            intent.putExtra(TITLE_KEY, title)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }
    }
}
