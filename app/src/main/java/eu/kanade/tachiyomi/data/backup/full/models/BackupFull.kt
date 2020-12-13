package eu.kanade.tachiyomi.data.backup.full.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupFull {
    fun getDefaultFilename(): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        return "tachiyomi_$date.proto.gz"
    }
}
