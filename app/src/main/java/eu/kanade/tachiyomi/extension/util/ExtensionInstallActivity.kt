package eu.kanade.tachiyomi.extension.util

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Activity used to install extensions, because we can only receive the result of the installation
 * with [startActivityForResult], which we need to update the UI.
 */
class ExtensionInstallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(intent.data, intent.type)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivityForResult(installIntent, INSTALL_REQUEST_CODE)
        } catch (error: Exception) {
            // Either install package can't be found (probably bots) or there's a security exception
            // with the download manager. Nothing we can workaround.
            toast(error.message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == INSTALL_REQUEST_CODE) {
            checkInstallationResult(resultCode)
        }
        finish()
    }

    private fun checkInstallationResult(resultCode: Int) {
        val downloadId = intent.extras!!.getLong(ExtensionInstaller.EXTRA_DOWNLOAD_ID)
        val success = resultCode == RESULT_OK

        val extensionManager = Injekt.get<ExtensionManager>()
        extensionManager.setInstallationResult(downloadId, success)
    }

    private companion object {
        const val INSTALL_REQUEST_CODE = 500
    }
}
