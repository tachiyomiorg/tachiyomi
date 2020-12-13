package eu.kanade.tachiyomi.ui.setting.track

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.webview.BaseWebViewActivity
import eu.kanade.tachiyomi.util.system.WebViewClientCompat
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

class MyAnimeListLoginActivity : BaseWebViewActivity() {

    private val trackManager: TrackManager by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (bundle == null) {
            binding.webview.webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Get CSRF token from HTML after post-login redirect
                    if (url == "https://myanimelist.net/") {
                        view?.evaluateJavascript(
                            "(function(){return document.querySelector('meta[name=csrf_token]').getAttribute('content')})();"
                        ) {
                            trackManager.myAnimeList.login(it.replace("\"", ""))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                    {
                                        returnToSettings()
                                    },
                                    {
                                        returnToSettings()
                                    }
                                )
                        }
                    }
                }
            }

            binding.webview.loadUrl(MyAnimeListApi.loginUrl())
        }
    }

    private fun returnToSettings() {
        finish()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MyAnimeListLoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(TITLE_KEY, context.getString(R.string.login))
            }
        }
    }
}
