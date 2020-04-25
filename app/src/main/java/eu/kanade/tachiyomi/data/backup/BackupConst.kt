package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

object BackupConst {

    const val INTENT_FILTER = "SettingsBackupFragment"
    const val ACTION_BACKUP_COMPLETED = "$ID.$INTENT_FILTER.ACTION_BACKUP_COMPLETED"
    const val ACTION_BACKUP_ERROR = "$ID.$INTENT_FILTER.ACTION_BACKUP_ERROR"
    const val ACTION = "$ID.$INTENT_FILTER.ACTION"
    const val EXTRA_ERROR_MESSAGE = "$ID.$INTENT_FILTER.EXTRA_ERROR_MESSAGE"
    const val EXTRA_URI = "$ID.$INTENT_FILTER.EXTRA_URI"
}
