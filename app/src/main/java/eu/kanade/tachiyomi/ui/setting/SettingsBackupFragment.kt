package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.hippo.unifile.UniFile
import com.nononsenseapps.filepicker.FilePickerActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupCreateService
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupRestoreService
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.*
import eu.kanade.tachiyomi.widget.CustomLayoutPickerActivity
import eu.kanade.tachiyomi.widget.preference.IntListPreference
import net.xpece.android.support.preference.Preference
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Settings for [BackupCreateService] and [BackupRestoreService]
 */
class SettingsBackupFragment : SettingsFragment() {

    companion object {
        const val INTENT_FILTER = "SettingsBackupFragment"
        const val ACTION_BACKUP_COMPLETED_DIALOG = "$ID.$INTENT_FILTER.ACTION_BACKUP_COMPLETED_DIALOG"
        const val ACTION_SET_PROGRESS_DIALOG = "$ID.$INTENT_FILTER.ACTION_SET_PROGRESS_DIALOG"
        const val ACTION_ERROR_BACKUP_DIALOG = "$ID.$INTENT_FILTER.ACTION_ERROR_BACKUP_DIALOG"
        const val ACTION_ERROR_RESTORE_DIALOG = "$ID.$INTENT_FILTER.ACTION_ERROR_RESTORE_DIALOG"
        const val ACTION_RESTORE_COMPLETED_DIALOG = "$ID.$INTENT_FILTER.ACTION_RESTORE_COMPLETED_DIALOG"
        const val ACTION = "$ID.$INTENT_FILTER.ACTION"
        const val EXTRA_PROGRESS = "$ID.$INTENT_FILTER.EXTRA_PROGRESS"
        const val EXTRA_AMOUNT = "$ID.$INTENT_FILTER.EXTRA_AMOUNT"
        const val EXTRA_ERRORS = "$ID.$INTENT_FILTER.EXTRA_ERRORS"
        const val EXTRA_CONTENT = "$ID.$INTENT_FILTER.EXTRA_CONTENT"
        const val EXTRA_ERROR_MESSAGE = "$ID.$INTENT_FILTER.EXTRA_ERROR_MESSAGE"
        const val EXTRA_URI = "$ID.$INTENT_FILTER.EXTRA_URI"
        const val EXTRA_TIME = "$ID.$INTENT_FILTER.EXTRA_TIME"
        const val EXTRA_ERROR_FILE_PATH = "$ID.$INTENT_FILTER.EXTRA_ERROR_FILE_PATH"
        const val EXTRA_ERROR_FILE = "$ID.$INTENT_FILTER.EXTRA_ERROR_FILE"

        private const val BACKUP_CREATE = 201
        private const val BACKUP_RESTORE = 202
        private const val BACKUP_DIR = 203

        fun newInstance(rootKey: String): SettingsBackupFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsBackupFragment().apply { arguments = args }
        }
    }

    /**
     * Preference selected to create backup
     */
    private val createBackup: Preference by bindPref(R.string.pref_create_local_backup_key)

    /**
     * Preference selected to restore backup
     */
    private val restoreBackup: Preference by bindPref(R.string.pref_restore_local_backup_key)

    /**
     * Preference which determines the frequency of automatic backups.
     */
    private val automaticBackup: IntListPreference by bindPref(R.string.pref_backup_interval_key)

    /**
     * Preference containing number of automatic backups
     */
    private val backupSlots: IntListPreference by bindPref(R.string.pref_backup_slots_key)

    /**
     * Preference containing interval of automatic backups
     */
    private val backupDirPref: Preference by bindPref(R.string.pref_backup_directory_key)

    /**
     * Preferences
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Value containing information on what to backup
     */
    private var backup_flags = 0

    /**
     * The root directory for backups..
     */
    private var backupDir = preferences.backupsDirectory().getOrDefault().let {
        UniFile.fromUri(context, Uri.parse(it))
    }

    val restoreDialog: MaterialDialog by lazy {
        MaterialDialog.Builder(context)
                .title(R.string.backup)
                .content(R.string.restoring_backup)
                .progress(false, 100, true)
                .cancelable(false)
                .negativeText(R.string.action_stop)
                .onNegative { materialDialog, _ ->
                    BackupRestoreService.stop(context)
                    materialDialog.dismiss()
                }
                .build()
    }

    val backupDialog: MaterialDialog by lazy {
        MaterialDialog.Builder(context)
                .title(R.string.backup)
                .content(R.string.creating_backup)
                .progress(true, 0)
                .cancelable(false)
                .build()
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra(ACTION)) {
                ACTION_BACKUP_COMPLETED_DIALOG -> {
                    backupDialog.dismiss()
                    val uri = Uri.parse(intent.getStringExtra(EXTRA_URI))
                    val file = UniFile.fromUri(context, uri)
                    MaterialDialog.Builder(this@SettingsBackupFragment.context)
                            .title(getString(R.string.backup_created))
                            .content(getString(R.string.file_saved, file.filePath))
                            .positiveText(getString(R.string.action_close))
                            .negativeText(getString(R.string.action_export))
                            .onPositive { materialDialog, _ -> materialDialog.dismiss() }
                            .onNegative { _, _ ->
                                val sendIntent = Intent(Intent.ACTION_SEND)
                                sendIntent.type = "application/json"
                                sendIntent.putExtra(Intent.EXTRA_STREAM, file.uri)
                                startActivity(Intent.createChooser(sendIntent, ""))
                            }
                            .show()

                }
                ACTION_SET_PROGRESS_DIALOG -> {
                    val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                    val amount = intent.getIntExtra(EXTRA_AMOUNT, 0)
                    val content = intent.getStringExtra(EXTRA_CONTENT)
                    restoreDialog.setContent(content)
                    restoreDialog.setProgress(progress)
                    restoreDialog.maxProgress = amount
                }
                ACTION_RESTORE_COMPLETED_DIALOG -> {
                    restoreDialog.dismiss()
                    val time = intent.getLongExtra(EXTRA_TIME, 0)
                    val errors = intent.getIntExtra(EXTRA_ERRORS, 0)
                    val path = intent.getStringExtra(EXTRA_ERROR_FILE_PATH)
                    val file = intent.getStringExtra(EXTRA_ERROR_FILE)
                    val timeString = String.format("%02d min, %02d sec",
                            TimeUnit.MILLISECONDS.toMinutes(time),
                            TimeUnit.MILLISECONDS.toSeconds(time) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
                    )

                    if (errors > 0) {
                        MaterialDialog.Builder(this@SettingsBackupFragment.context)
                                .title(getString(R.string.restore_completed))
                                .content(getString(R.string.restore_completed_content, timeString,
                                        if (errors > 0) "$errors" else getString(android.R.string.no)))
                                .positiveText(getString(R.string.action_close))
                                .negativeText(getString(R.string.action_open_log))
                                .onPositive { materialDialog, _ -> materialDialog.dismiss() }
                                .onNegative { materialDialog, _ ->
                                    if (!path.isEmpty()) {
                                        val destFile = File(path, file)
                                        val uri = destFile.getUriCompat(context)
                                        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "text/plain")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        startActivity(sendIntent)
                                    } else {
                                        context.toast(getString(R.string.error_opening_log))
                                    }
                                    materialDialog.dismiss()
                                }
                                .show()
                    }
                }
                ACTION_ERROR_BACKUP_DIALOG -> {
                    context.toast(intent.getStringExtra(EXTRA_ERROR_MESSAGE))
                    backupDialog.dismiss()
                }
                ACTION_ERROR_RESTORE_DIALOG -> {
                    context.toast(intent.getStringExtra(EXTRA_ERROR_MESSAGE))
                    restoreDialog.dismiss()
                }
            }
        }

    }

    override fun onPause() {
        context.unregisterLocalReceiver(receiver)
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        context.registerLocalReceiver(receiver, IntentFilter(INTENT_FILTER))
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        (activity as BaseActivity).requestPermissionsOnMarshmallow()

        // Set onClickListeners
        createBackup.setOnPreferenceClickListener {
            MaterialDialog.Builder(context)
                    .title(R.string.pref_create_backup)
                    .content(R.string.backup_choice)
                    .items(R.array.backup_options)
                    .itemsCallbackMultiChoice(arrayOf(0, 1, 2, 3, 4 /*todo not hard code*/)) { _, positions, _ ->
                        // TODO not very happy with global value, but putExtra doesn't work
                        backup_flags = 0
                        for (i in 1..positions.size - 1) {
                            when (positions[i]) {
                                1 -> backup_flags = backup_flags or BackupCreateService.BACKUP_CATEGORY
                                2 -> backup_flags = backup_flags or BackupCreateService.BACKUP_CHAPTER
                                3 -> backup_flags = backup_flags or BackupCreateService.BACKUP_TRACK
                                4 -> backup_flags = backup_flags or BackupCreateService.BACKUP_HISTORY
                            }
                        }
                        // If API lower as KitKat use custom dir picker
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                            // Get dirs
                            val currentDir = preferences.backupsDirectory().getOrDefault()

                            val i = Intent(activity, CustomLayoutPickerActivity::class.java)
                            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
                            i.putExtra(FilePickerActivity.EXTRA_START_PATH, currentDir)
                            startActivityForResult(i, BACKUP_CREATE)
                        } else {
                            // Use Androids build in file creator
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            intent.addCategory(Intent.CATEGORY_OPENABLE)

                            // TODO create custom MIME data type? Will make older backups deprecated
                            intent.type = "application/*"
                            intent.putExtra(Intent.EXTRA_TITLE, Backup.getDefaultFilename())
                            startActivityForResult(intent, BACKUP_CREATE)
                        }
                        true
                    }
                    .itemsDisabledIndices(0)
                    .positiveText(getString(R.string.action_create))
                    .negativeText(android.R.string.cancel)
                    .show()
            true
        }

        restoreBackup.setOnPreferenceClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                val intent = Intent()
                intent.type = "application/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, getString(R.string.file_select_backup)), BACKUP_RESTORE)
            } else {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "application/*"
                startActivityForResult(intent, BACKUP_RESTORE)
            }
            true
        }

        automaticBackup.setOnPreferenceChangeListener { _, newValue ->
            // Always cancel the previous task, it seems that sometimes they are not updated.
            BackupCreatorJob.cancelTask()

            val interval = (newValue as String).toInt()
            if (interval > 0) {
                BackupCreatorJob.setupTask(interval)
            }
            true
        }

        backupSlots.setOnPreferenceChangeListener { preference, newValue ->
            preferences.numberOfBackups().set((newValue as String).toInt())
            preference.summary = newValue
            true
        }

        backupDirPref.setOnPreferenceClickListener {
            val currentDir = preferences.backupsDirectory().getOrDefault()

            if (Build.VERSION.SDK_INT < 21) {
                // Custom dir selected, open directory selector
                val i = Intent(activity, CustomLayoutPickerActivity::class.java)
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, currentDir)

                startActivityForResult(i, BACKUP_DIR)
            } else {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(i, BACKUP_DIR)
            }

            true
        }

        subscriptions += preferences.backupsDirectory().asObservable()
                .subscribe { path ->
                    backupDir = UniFile.fromUri(context, Uri.parse(path))
                    backupDirPref.summary = backupDir.filePath ?: path
                }

        subscriptions += preferences.backupInterval().asObservable()
                .subscribe {
                    backupDirPref.isVisible = it > 0
                    backupSlots.isVisible = it > 0
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            BACKUP_DIR -> if (data != null && resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    val uri = Uri.fromFile(File(data.data.path))
                    preferences.backupsDirectory().set(uri.toString())
                } else {
                    val uri = data.data
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    context.contentResolver.takePersistableUriPermission(uri, flags)

                    val file = UniFile.fromUri(context, uri)
                    preferences.backupsDirectory().set(file.uri.toString())
                }
            }
            BACKUP_CREATE -> if (data != null && resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    val dir = data.data.path
                    val file = File(dir, Backup.getDefaultFilename())

                    backupDialog.show()
                    BackupCreateService.makeBackup(context, file.toURI().toString(), backup_flags)
                } else {
                    val uri = data.data
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    val file = UniFile.fromUri(context, uri)

                    backupDialog.show()
                    BackupCreateService.makeBackup(context, file.uri.toString(), backup_flags)
                }
            }
            BACKUP_RESTORE -> if (data != null && resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    val uri = Uri.fromFile(File(data.data.path))

                    MaterialDialog.Builder(context)
                            .title(getString(R.string.pref_restore_backup))
                            .content(getString(R.string.backup_restore_content))
                            .positiveText(getString(R.string.action_restore))
                            .onPositive { materialDialog, _ ->
                                materialDialog.dismiss()
                                restoreDialog.show()
                                BackupRestoreService.start(context, uri.toString())
                            }
                            .show()
                } else {
                    val uri = data.data
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    val file = UniFile.fromUri(context, uri)

                    MaterialDialog.Builder(context)
                            .title(getString(R.string.pref_restore_backup))
                            .content(getString(R.string.backup_restore_content))
                            .positiveText(getString(R.string.action_restore))
                            .onPositive { materialDialog, _ ->
                                materialDialog.dismiss()
                                restoreDialog.show()
                                BackupRestoreService.start(context, file.uri.toString())
                            }
                            .show()
                }
            }
        }
    }

}