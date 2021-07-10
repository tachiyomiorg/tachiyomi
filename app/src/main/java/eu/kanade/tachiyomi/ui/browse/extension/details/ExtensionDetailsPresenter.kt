package eu.kanade.tachiyomi.ui.browse.extension.details

import android.os.Bundle
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.browse.BrowseController
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUninstallWarnDialog
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrationMangaController
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

    fun findSourcesDependentOnExtension(pkgName: String) = extensionManager.findSourcesDependentOnExtension(pkgName)

    fun showExtensionWarnDialog(sources: List<Source>) {
        ExtensionUninstallWarnDialog(sources)
            .apply {
                uninstallCallback = ::uninstallExtension

                migrationCallback = {
                    router.popCurrentController()
                    // Only 1 source: directly open MigrationBrowseController
                    if (sources.size == 1) {
                        val targetSource = sources[0]
                        router.pushController(MigrationMangaController(targetSource.id, targetSource.name).withFadeTransaction())
                    } else {
                        router.popCurrentController()

                        val browseController = router.backstack.last().controller() as BrowseController
                        browseController.setActiveTab(BrowseController.MIGRATION_CONTROLLER)
                    }
                }
            }
            .showDialog(view!!.router)
    }

    fun uninstallExtension() {
        val extension = extension ?: return
        extensionManager.uninstallExtension(extension.pkgName)
    }
}
