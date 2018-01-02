package eu.kanade.tachiyomi.data.sync.protocol.category

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.FileNotFoundException

class CategorySnapshotHelper(private val context: Context) {
    val db: DatabaseHelper by injectLazy()
    
    private fun snapshotFile(id: String)
        = File(context.filesDir, "sync_categories_$id.snapshot")
    
    fun takeCategorySnapshots(id: String) {
        //Take snapshots
        val snapshots = db.getCategories().executeAsBlocking().map {
            CategorySnapshot(it).serialize()
        }
        
        //Write snapshots to disk
        snapshotFile(id).writeText(snapshots.joinToString("\n"), CHARSET)
    }
    
    fun readCategorySnapshots(id: String): List<CategorySnapshot> {
        //Read snapshots from disk
        return try {
            snapshotFile(id).useLines(CHARSET) {
                it.filterNot(String::isBlank).map {
                    CategorySnapshot.deserialize(it)
                }.toList()
            }
        } catch(e: FileNotFoundException) {
            emptyList()
        }
    }
    
    fun deleteCategorySnapshots(id: String) {
        snapshotFile(id).delete()
    }
    
    companion object {
        private val CHARSET = Charsets.UTF_8
    }
}