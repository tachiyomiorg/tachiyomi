package eu.kanade.tachiyomi.ui.base.activity

import android.support.v7.app.AppCompatActivity
import eu.kanade.tachiyomi.util.LocaleHelper

abstract class BaseActivity : AppCompatActivity(), ActivityMixin {

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

}
