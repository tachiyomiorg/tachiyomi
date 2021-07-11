package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper

abstract class BaseViewBindingActivity<VB : ViewBinding> : BaseThemedActivity() {

    lateinit var binding: VB

    @Suppress("LeakingThis")
    private val secureActivityDelegate = SecureActivityDelegate(this)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.createLocaleWrapper(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        secureActivityDelegate.onCreate()
    }

    override fun onResume() {
        super.onResume()

        secureActivityDelegate.onResume()
    }
}
