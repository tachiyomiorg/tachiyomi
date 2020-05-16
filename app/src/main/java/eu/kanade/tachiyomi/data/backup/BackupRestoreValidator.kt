package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.Backup

object BackupRestoreValidator {

    /**
     * Checks for critical backup file data.
     *
     * @throws Exception if version or manga cannot be found.
     * @return List of required sources.
     */
    fun validate(context: Context, uri: Uri): Map<Long, String> {
        val reader = JsonReader(context.contentResolver.openInputStream(uri)!!.bufferedReader())
        val json = JsonParser.parseReader(reader).asJsonObject

        val version = json.get(Backup.VERSION)
        val mangasJson = json.get(Backup.MANGAS)
        if (version == null || mangasJson == null) {
            throw Exception(context.getString(R.string.invalid_backup_file_missing_data))
        }

        if (mangasJson.asJsonArray.size() == 0) {
            throw Exception(context.getString(R.string.invalid_backup_file_missing_manga))
        }

        return getSourceMapping(json)
    }

    fun getSourceMapping(json: JsonObject): Map<Long, String> {
        val extensionsMapping = json.get(Backup.EXTENSIONS) ?: return emptyMap()

        return extensionsMapping.asJsonArray
            .map {
                val items = it.asString.split(":")
                items[0].toLong() to items[1]
            }
            .toMap()
    }
}
