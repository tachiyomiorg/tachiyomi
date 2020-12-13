package eu.kanade.tachiyomi.ui.browse.extension.details

import android.os.Bundle
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionDetailsPresenter(
    val pkgName: String,
    private val extensionManager: ExtensionManager = Injekt.get()
) : BasePresenter<ExtensionDetailsController>() {

    val extension = extensionManager.installedExtensions.find { it.pkgName == pkgName }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        bindToUninstalledExtension()
    }

    private fun bindToUninstalledExtension() {
        extensionManager.getInstalledExtensionsObservable()
            .skip(1)
            .filter { extensions -> extensions.none { it.pkgName == pkgName } }
            .map { }
            .take(1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst({ view, _ ->
                view.onExtensionUninstalled()
            })
    }

    fun uninstallExtension() {
        val extension = extension ?: return
        extensionManager.uninstallExtension(extension.pkgName)
    }
}
