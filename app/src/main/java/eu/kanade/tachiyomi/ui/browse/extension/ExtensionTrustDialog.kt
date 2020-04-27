package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ExtensionTrustDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : ExtensionTrustDialog.Listener {

    constructor(target: T, signatureHash: String, pkgName: String) : this(
        Bundle().apply {
            putString(SIGNATURE_KEY, signatureHash)
            putString(PKGNAME_KEY, pkgName)
        }
    ) {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.untrusted_extension)
            .message(R.string.untrusted_extension_message)
            .positiveButton(R.string.ext_trust) {
                (targetController as? Listener)?.trustSignature(args.getString(SIGNATURE_KEY)!!)
            }
            .negativeButton(R.string.ext_uninstall) {
                (targetController as? Listener)?.uninstallExtension(args.getString(PKGNAME_KEY)!!)
            }
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
