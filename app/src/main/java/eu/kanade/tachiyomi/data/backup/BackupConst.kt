package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

object BackupConst {

    private const val NAME = "BackupRestoreServices"
    const val EXTRA_URI = "$ID.$NAME.EXTRA_URI"
    const val EXTRA_FLAGS = "$ID.$NAME.EXTRA_FLAGS"
}
