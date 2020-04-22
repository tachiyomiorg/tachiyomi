package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.pm.PackageManager
import android.webkit.WebView

object WebViewUtil {
    val WEBVIEW_UA_VERSION_REGEX by lazy {
        Regex(""".*Chrome/(\d+)\..*""")
    }

    const val MINIMUM_WEBVIEW_VERSION = 79

    fun supportsWebView(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
    }
}

fun WebView.isOutdated(): Boolean {
    return getWebViewMajorVersion(this) < WebViewUtil.MINIMUM_WEBVIEW_VERSION
}

// Based on https://stackoverflow.com/a/29218966
private fun getWebViewMajorVersion(webview: WebView): Int {
    val originalUA: String = webview.settings.userAgentString

    // Next call to getUserAgentString() will get us the default
    webview.settings.userAgentString = null

    val uaRegexMatch = WebViewUtil.WEBVIEW_UA_VERSION_REGEX.matchEntire(webview.settings.userAgentString)
    val webViewVersion: Int = if (uaRegexMatch != null && uaRegexMatch.groupValues.size > 1) {
        uaRegexMatch.groupValues[1].toInt()
    } else {
        0
    }

    // Revert to original UA string
    webview.settings.userAgentString = originalUA

    return webViewVersion
}
