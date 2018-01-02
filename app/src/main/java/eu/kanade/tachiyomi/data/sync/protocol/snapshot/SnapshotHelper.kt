package eu.kanade.tachiyomi.data.sync.protocol.snapshot

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.FileNotFoundException

class SnapshotHelper(private val context: Context) {
    val db: DatabaseHelper by injectLazy()
    
    private fun snapshotFile(type: String, id: String)
        = File(context.filesDir, "sync_${type}_$id.snapshot")
    
    fun takeSnapshots(id: String) {
        //Take snapshots
        val categorySnapshots = db.getCategories().executeAsBlocking().map {
            CategorySnapshot(it).serialize()
        }
        val trackSnapshots = db.getTracks().executeAsBlocking().map {
            TrackSnapshot(it).serialize()
        }
        
        //Write snapshots to disk
        writeSnapshot(CATEGORY_SNAPSHOTS, id, categorySnapshots)
        writeSnapshot(TRACK_SNAPSHOTS, id, trackSnapshots)
    }
    
    private fun writeSnapshot(type: String, id: String, snapshots: List<String>) {
        snapshotFile(type, id).writeText(snapshots.joinToString("\n"), CHARSET)
    }
    
    fun readCategorySnapshots(id: String): List<CategorySnapshot>
        = baseReadSnapshots(CATEGORY_SNAPSHOTS, id) {
        CategorySnapshot.deserialize(it)
    }
    
    fun readTrackSnapshots(id: String): List<TrackSnapshot>
            = baseReadSnapshots(TRACK_SNAPSHOTS, id) {
        TrackSnapshot.deserialize(it)
    }
    
    private fun <R> baseReadSnapshots(type: String, id: String, deserializer: (String) -> R): List<R> {
        //Read snapshots from disk
        return try {
            snapshotFile(type, id).useLines(CHARSET) {
                it.filterNot(String::isBlank).map(deserializer).toList()
            }
        } catch(e: FileNotFoundException) {
            emptyList()
        }
    }
    
    fun deleteSnapshots(id: String) {
        snapshotFile(CATEGORY_SNAPSHOTS, id).delete()
        snapshotFile(TRACK_SNAPSHOTS, id).delete()
    }
    
    companion object {
        private val CHARSET = Charsets.UTF_8
        private val CATEGORY_SNAPSHOTS = "categories"
        private val TRACK_SNAPSHOTS = "tracks"
    }
}