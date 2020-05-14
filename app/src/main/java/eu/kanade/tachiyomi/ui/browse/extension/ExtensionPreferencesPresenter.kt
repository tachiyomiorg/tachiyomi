package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionPreferencesPresenter(
    val pkgName: String,
    extensionManager: ExtensionManager = Injekt.get()
) : BasePresenter<ExtensionPreferencesController>() {

    val extension = extensionManager.installedExtensions.find { it.pkgName == pkgName }
}
