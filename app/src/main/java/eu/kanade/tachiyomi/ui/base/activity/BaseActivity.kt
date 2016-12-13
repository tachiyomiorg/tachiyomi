package eu.kanade.tachiyomi.ui.base.activity

import android.support.v7.app.AppCompatActivity
import eu.kanade.tachiyomi.util.LocaleHelper

abstract class BaseActivity : AppCompatActivity(), ActivityMixin {

    override fun getActivity() = this
    init {
        LocaleHelper.updateCfg(this)
    }

}
