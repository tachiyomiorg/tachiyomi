package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupManager
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.util.isNullOrUnsubscribed
import eu.kanade.tachiyomi.util.toast
import net.xpece.android.support.preference.Preference
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class SettingsBackupFragment : SettingsFragment() {

    val createBackup by lazy {
        findPreference(getString(R.string.pref_create_local_backup_key)) as Preference
    }

    val restoreBackup by lazy {
        findPreference(getString(R.string.pref_restore_local_backup_key)) as Preference
    }

    /**
     * Database.
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Backup manager.
     */
    private lateinit var backupManager: BackupManager

    /**
     * Subscription where the backup is restored.
     */
    private var restoreSubscription: Subscription? = null

    /**
     * Subscription where the backup is created.
     */
    private var backupSubscription: Subscription? = null

    /**
     * Dialog shown while creating backup
     */
    private var backupDialog: Dialog? = null

    /**
     * Dialog shown while restoring backup
     */
    private var restoreDialog: Dialog? = null


    companion object {
        const private val REQUEST_BACKUP_OPEN = 102

        fun newInstance(rootKey: String): SettingsBackupFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsBackupFragment().apply { arguments = args }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backupManager = BackupManager(db)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        // Set onClickListeners
        createBackup.setOnPreferenceClickListener {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val file = File(activity.externalCacheDir, "tachiyomi-$today.json")
            createBackup(file)
            backupDialog = MaterialDialog.Builder(activity)
                    .content(R.string.backup_please_wait)
                    .progress(true, 0)
                    .show()
            true
        }

        restoreBackup.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/*"
            val chooser = Intent.createChooser(intent, getString(R.string.file_select_backup))
            startActivityForResult(chooser, REQUEST_BACKUP_OPEN)
            true
        }
    }

    /**
     * Creates a backup and saves it to a file.
     * @param file the path where the file will be saved.
     */
    fun createBackup(file: File) {
        if (backupSubscription.isNullOrUnsubscribed()) {
            backupSubscription = getBackupObservable(file)
                    .take(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ onBackupCompleted(file) }, { onBackupError(it) })
        }
    }

    /**
     * Returns the observable to save a backup.
     */
    private fun getBackupObservable(file: File) = Observable.fromCallable {
        backupManager.backupToFile(file)
        true
    }

    /**
     * Called when the backup is completed.
     * @param file the file where the backup is saved.
     */
    fun onBackupCompleted(file: File) {
        dismissBackupDialog()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file))
        startActivity(Intent.createChooser(intent, ""))
    }

    /**
     * Called when there's an error doing the backup.
     * @param error the exception thrown.
     */
    fun onBackupError(error: Throwable) {
        dismissBackupDialog()
        context.toast(error.message)
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
     * Restores a backup from a stream.
     * @param stream the input stream of the backup file.
     */
    fun restoreBackup(stream: InputStream) {
        if (restoreSubscription.isNullOrUnsubscribed()) {
            restoreSubscription = getRestoreObservable(stream)
                    .take(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ onRestoreCompleted() }, { onRestoreError(it) })
        }
    }

    /**
     * Returns the observable to restore a backup.
     */
    private fun getRestoreObservable(stream: InputStream) = Observable.fromCallable {
        backupManager.restoreFromStream(stream)
        true
    }

    /**
     * Called when the restore is completed.
     */
    fun onRestoreCompleted() {
        dismissRestoreDialog()
        context.toast(R.string.backup_completed)
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

    /**
     * Called when there's an error restoring the backup.
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

            // When using cloud services, we have to open the input stream in a background thread.
            Observable.fromCallable { context.contentResolver.openInputStream(data.data) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        restoreBackup(it)
                    }, { error ->
                        context.toast(error.message)
                        Timber.e(error)
                    })
                    .apply { subscriptions.add(this) }

        }
    }

}
