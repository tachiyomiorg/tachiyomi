package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

object BackupConst {

    private const val NAME = "BackupRestoreServices"
    const val EXTRA_URI = "$ID.$NAME.EXTRA_URI"
    const val EXTRA_FLAGS = "$ID.$NAME.EXTRA_FLAGS"
    const val EXTRA_MODE = "$ID.$NAME.EXTRA_MODE"

    const val BACKUP_TYPE_LEGACY = 0
    const val BACKUP_TYPE_FULL = 1
}
