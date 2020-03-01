package eu.kanade.tachiyomi.ui.base.presenter

import android.os.Bundle
import nucleus.factory.PresenterFactory
import nucleus.presenter.Presenter

class NucleusConductorDelegate<P : Presenter<*>>(private val factory: PresenterFactory<P>) {

    var presenter: P? = null
        get() {
            if (field == null) {
                field = factory.createPresenter()
                field!!.create(bundle)
                bundle = null
            }
            return field
        }

    private var bundle: Bundle? = null

    fun onSaveInstanceState(): Bundle {
        val bundle = Bundle()
        //        getPresenter(); // Workaround a crash related to saving instance state with child routers
        presenter?.save(bundle)
        return bundle
    }

    fun onRestoreInstanceState(presenterState: Bundle?) {
        bundle = presenterState
    }

    @Suppress("UNCHECKED_CAST")
    private fun <View> Presenter<View>.takeView(view: Any) = takeView(view as View)

    fun onTakeView(view: Any) {
        presenter?.takeView(view)
    }

    fun onDropView() {
        presenter?.dropView()
    }

    fun onDestroy() {
        presenter?.destroy()
    }
}
