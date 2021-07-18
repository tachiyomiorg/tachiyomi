package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ExtensionTrustDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : ExtensionTrustDialog.Listener {

    constructor(target: T, signatureHash: String, pkgName: String) : this(
        bundleOf(
            SIGNATURE_KEY to signatureHash,
            PKGNAME_KEY to pkgName
        )
    ) {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.untrusted_extension)
            .setMessage(R.string.untrusted_extension_message)
            .setPositiveButton(R.string.ext_trust) { _, _ ->
                (targetController as? Listener)?.trustSignature(args.getString(SIGNATURE_KEY)!!)
            }
            .setNegativeButton(R.string.ext_uninstall) { _, _ ->
                (targetController as? Listener)?.uninstallExtension(args.getString(PKGNAME_KEY)!!)
            }
            .create()
    }

    private companion object {
        const val SIGNATURE_KEY = "signature_key"
        const val PKGNAME_KEY = "pkgname_key"
    }

    interface Listener {
        fun trustSignature(signatureHash: String)
        fun uninstallExtension(pkgName: String)
    }
}
