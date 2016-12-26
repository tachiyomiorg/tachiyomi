package eu.kanade.tachiyomi.ui.base.activity

import android.support.v7.app.AppCompatActivity
import eu.kanade.tachiyomi.util.LocaleHelper

abstract class BaseActivity : AppCompatActivity(), ActivityMixin {

    init {
        LocaleHelper.updateConfiguration(this)
    }

    override fun getActivity() = this

    var resumed = false
        private set

    override fun onResume() {
        super.onResume()
        resumed = true
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

}
