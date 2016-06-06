package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ProgressBar
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

class AnilistLoginActivity : AppCompatActivity() {

    val syncManager: MangaSyncManager by injectLazy()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        val view = ProgressBar(this)
        setContentView(view, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, CENTER))

        val code = intent.data?.getQueryParameter("code")
        if (code != null) {
            syncManager.aniList.login(code)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ error ->
                        returnToSettings()
                    }, {
                        returnToSettings()
                    })
        } else {
            syncManager.aniList.logout()
            returnToSettings()
        }
    }

    private fun returnToSettings() {
        finish()

        val intent = Intent(this, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

}