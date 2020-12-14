package eu.kanade.tachiyomi.ui.webview

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.WebviewActivityBinding
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.navigationClicks
import reactivecircus.flowbinding.swiperefreshlayout.refreshes

open class BaseWebViewActivity : BaseActivity<WebviewActivityBinding>() {

    internal var bundle: Bundle? = null

    internal var isRefreshing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!WebViewUtil.supportsWebView(this)) {
            toast(R.string.information_webview_required, Toast.LENGTH_LONG)
            finish()
            return
        }

        try {
            binding = WebviewActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Throwable) {
            // Potentially throws errors like "Error inflating class android.webkit.WebView"
            toast(R.string.information_webview_required, Toast.LENGTH_LONG)
            finish()
            return
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
            binding.webview.setDefaultSettings()

            // Debug mode (chrome://inspect/#devices)
            if (BuildConfig.DEBUG && 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            binding.webview.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.isVisible = true
                    binding.progressBar.progress = newProgress
                    if (newProgress == 100) {
                        binding.progressBar.isInvisible = true
                    }
                    super.onProgressChanged(view, newProgress)
                }
            }
        } else {
            binding.webview.restoreState(bundle)
        }
    }

    override fun onDestroy() {
        binding.webview?.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) binding.webview.goBack()
        else super.onBackPressed()
    }

    fun refreshPage() {
        binding.swipeRefresh.isRefreshing = true
        binding.webview.reload()
        isRefreshing = true
    }

    companion object {
        const val TITLE_KEY = "title_key"
    }
}
