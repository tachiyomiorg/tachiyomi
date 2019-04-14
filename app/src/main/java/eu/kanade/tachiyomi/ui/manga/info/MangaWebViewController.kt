package eu.kanade.tachiyomi.ui.manga.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.util.WebViewClientCompat
import uy.kohesive.injekt.injectLazy

class MangaWebViewController(bundle: Bundle? = null) : BaseController(bundle) {

    private val sourceManager by injectLazy<SourceManager>()

    constructor(sourceId: Long, url: String) : this(Bundle().apply {
        putLong(SOURCE_KEY, sourceId)
        putString(URL_KEY, url)
    })

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.manga_info_web_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        val source = sourceManager.get(args.getLong(SOURCE_KEY)) as? HttpSource ?: return
        val url = args.getString(URL_KEY) ?: return
        val headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

        val web = view as WebView
        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }
        web.settings.javaScriptEnabled = true
        web.settings.userAgentString = source.headers["User-Agent"]
        web.loadUrl(url, headers)
    }

    override fun onDestroyView(view: View) {
        val web = view as WebView
        web.stopLoading()
        web.destroy()
        super.onDestroyView(view)
    }

    private companion object {
        const val SOURCE_KEY = "source_key"
        const val URL_KEY = "url_key"
    }

}
