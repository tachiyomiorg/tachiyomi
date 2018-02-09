package eu.kanade.tachiyomi.data.sync.protocol.snapshot

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileNotFoundException

/**
 * Helper class used to generate and manage snapshots
 */
class SnapshotHelper(private val context: Context,
                     private val db: DatabaseHelper = Injekt.get()) {

    /**
     * The file holding a snapshot for objects of [type] and for the
     * specified [id]
     */
    private fun snapshotFile(type: String, id: String)
        = File(context.filesDir, "sync_${type}_$id.snapshot")
    
    /**
     * Take all snapshots for the provided device
     */
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
    
    /**
     * Write a snapshot to disk
     */
    private fun writeSnapshot(type: String, id: String, snapshots: List<String>) {
        snapshotFile(type, id).writeText(snapshots.joinToString("\n"), CHARSET)
    }
    
    /**
     * Read the [CategorySnapshot] for the specified device
     */
    fun readCategorySnapshots(id: String): List<CategorySnapshot>
        = baseReadSnapshots(CATEGORY_SNAPSHOTS, id) {
        CategorySnapshot.deserialize(it)
    }
    
    /**
     * Read the [TrackSnapshot] for the specified device
     */
    fun readTrackSnapshots(id: String): List<TrackSnapshot>
            = baseReadSnapshots(TRACK_SNAPSHOTS, id) {
        TrackSnapshot.deserialize(it)
    }
    
    /**
     * Read a set of snapshots from disk
     *
     * @param R The type of snapshots to read
     */
    private fun <R> baseReadSnapshots(type: String, id: String, deserializer: (String) -> R): List<R> {
        //Read snapshots from disk
        return try {
            snapshotFile(type, id).useLines(CHARSET) {
                it.filterNot(String::isBlank).map(deserializer).toList()
            }
        } catch(e: FileNotFoundException) {
            //No previous snapshot, return empty list
            emptyList()
        }
    }
    
    /**
     * Delete the snapshots for a device
     */
    fun deleteSnapshots(id: String) {
        snapshotFile(CATEGORY_SNAPSHOTS, id).delete()
        snapshotFile(TRACK_SNAPSHOTS, id).delete()
    }
    
    companion object {
        private val CHARSET = Charsets.UTF_8
        private const val CATEGORY_SNAPSHOTS = "categories"
        private const val TRACK_SNAPSHOTS = "tracks"
    }
}