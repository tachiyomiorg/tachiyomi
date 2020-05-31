package eu.kanade.tachiyomi.ui.browse.extension

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ExtensionDetailControllerBinding
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks

class ExtensionDetailsController(bundle: Bundle? = null) :
    NucleusController<ExtensionDetailControllerBinding, ExtensionDetailsPresenter>(bundle) {

    constructor(pkgName: String) : this(
        Bundle().apply {
            putString(PKGNAME_KEY, pkgName)
        }
    )

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = ExtensionDetailControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun createPresenter(): ExtensionDetailsPresenter {
        return ExtensionDetailsPresenter(args.getString(PKGNAME_KEY)!!)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_extension_info)
    }

    @SuppressLint("PrivateResource")
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        val extension = presenter.extension ?: return
        val context = view.context

        extension.getApplicationIcon(context)?.let { binding.extensionIcon.setImageDrawable(it) }
        binding.extensionTitle.text = extension.name
        binding.extensionVersion.text = context.getString(R.string.ext_version_info, extension.versionName)
        binding.extensionLang.text = context.getString(R.string.ext_language_info, LocaleHelper.getSourceDisplayName(extension.lang, context))
        binding.extensionPkg.text = extension.pkgName

        binding.extensionUninstallButton.clicks()
            .onEach { presenter.uninstallExtension() }
            .launchIn(scope)

        if (extension.isObsolete) {
            binding.extensionWarningBanner.visible()
            binding.extensionWarningBanner.setText(R.string.obsolete_extension_message)
        }

        if (extension.isUnofficial) {
            binding.extensionWarningBanner.visible()
            binding.extensionWarningBanner.setText(R.string.unofficial_extension_message)
        }

        if (presenter.extension?.sources?.find { it is ConfigurableSource } != null) {
            binding.extensionPrefs.visible()
            binding.extensionPrefs.clicks()
                .onEach { openPreferences() }
                .launchIn(scope)
        }
    }

    fun onExtensionUninstalled() {
        router.popCurrentController()
    }

    private fun openPreferences() {
        router.pushController(
            ExtensionPreferencesController(presenter.extension!!.pkgName).withFadeTransaction()
        )
    }

    private companion object {
        const val PKGNAME_KEY = "pkg_name"
    }
}
