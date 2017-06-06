package eu.kanade.tachiyomi.ui.base.activity

import android.support.v7.app.AppCompatActivity
import eu.kanade.tachiyomi.util.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
    }

}
