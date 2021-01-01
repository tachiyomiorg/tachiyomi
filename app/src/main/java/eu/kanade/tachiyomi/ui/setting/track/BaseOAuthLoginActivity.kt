package eu.kanade.tachiyomi.ui.setting.track

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import uy.kohesive.injekt.injectLazy

abstract class BaseOAuthLoginActivity : BaseThemedActivity() {

    internal val trackManager: TrackManager by injectLazy()

    abstract fun handleResult(data: Uri?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = ProgressBar(this)
        setContentView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        handleResult(intent.data)
    }

    internal fun returnToSettings() {
        finish()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}
