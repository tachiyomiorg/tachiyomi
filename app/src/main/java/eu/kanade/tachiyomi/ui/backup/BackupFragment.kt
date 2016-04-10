package eu.kanade.tachiyomi.ui.backup

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_backup.*
import nucleus.factory.RequiresPresenter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment to create and restore backups of the application's data.
 * Uses R.layout.fragment_backup.
 */
@RequiresPresenter(BackupPresenter::class)
class BackupFragment : BaseRxFragment<BackupPresenter>() {

    private var backupDialog: Dialog? = null
    private var restoreDialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_backup, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        backup_button.setOnClickListener {
            val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val file = File(activity.externalCacheDir, "tachiyomi-$today.json")
            presenter.createBackup(file)

            backupDialog = MaterialDialog.Builder(activity)
                    .content(R.string.backup_please_wait)
                    .progress(true, 0)
                    .show()
        }

        restore_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/octet-stream"
            val chooser = Intent.createChooser(intent, getString(R.string.file_select_cover))
            startActivityForResult(chooser, REQUEST_BACKUP_OPEN)
        }
    }

    /**
     * Called from the presenter when the backup is completed.
     *
     * @param file the file where the backup is saved.
     */
    fun onBackupCompleted(file: File) {
        dismissBackupDialog()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file))
        startActivity(Intent.createChooser(intent, ""))
    }

    /**
     * Called from the presenter when the restore is completed.
     */
    fun onRestoreCompleted() {
        dismissRestoreDialog()
        context.toast(R.string.backup_completed)
    }

    /**
     * Called from the presenter when there's an error doing the backup.
     * @param error the exception thrown.
     */
    fun onBackupError(error: Throwable) {
        dismissBackupDialog()
        context.toast(error.message)
    }

    /**
     * Called from the presenter when there's an error restoring the backup.
     * @param error the exception thrown.
     */
    fun onRestoreError(error: Throwable) {
        dismissRestoreDialog()
        context.toast(error.message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == REQUEST_BACKUP_OPEN) {
            restoreDialog = MaterialDialog.Builder(activity)
                    .content(R.string.restore_please_wait)
                    .progress(true, 0)
                    .show()

            val stream = context.contentResolver.openInputStream(data.data)
            presenter.restoreBackup(stream)
        }
    }

    /**
     * Dismisses the backup dialog.
     */
    fun dismissBackupDialog() {
        backupDialog?.let {
            it.dismiss()
            backupDialog = null
        }
    }

    /**
     * Dismisses the restore dialog.
     */
    fun dismissRestoreDialog() {
        restoreDialog?.let {
            it.dismiss()
            restoreDialog = null
        }
    }

    companion object {

        private val REQUEST_BACKUP_OPEN = 102

        fun newInstance(): BackupFragment {
            return BackupFragment()
        }
    }
}
