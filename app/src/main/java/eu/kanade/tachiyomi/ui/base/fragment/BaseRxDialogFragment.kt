package eu.kanade.tachiyomi.ui.base.fragment

import android.os.Bundle
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.NucleusSupportDialogFragment

abstract class BaseRxDialogFragment<P : BasePresenter<*>> : NucleusSupportDialogFragment<P>(), FragmentMixin {

    override fun onCreate(bundle: Bundle?) {
        val superFactory = presenterFactory
        setPresenterFactory {
            superFactory.createPresenter().apply {
                val app = activity.application as App
                context = app.applicationContext
            }
        }
        super.onCreate(bundle)
    }
}
