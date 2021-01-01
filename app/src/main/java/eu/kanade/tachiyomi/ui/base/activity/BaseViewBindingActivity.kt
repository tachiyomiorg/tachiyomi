package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper

abstract class BaseViewBindingActivity<VB : ViewBinding> : BaseThemedActivity() {

    val scope = lifecycleScope
    lateinit var binding: VB

    @Suppress("LeakingThis")
    private val secureActivityDelegate = SecureActivityDelegate(this)

    init {
        @Suppress("LeakingThis")
        LocaleHelper.updateConfiguration(this)
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
