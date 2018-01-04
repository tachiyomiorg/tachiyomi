package eu.kanade.tachiyomi.data.sync.protocol.models

import eu.kanade.tachiyomi.data.sync.protocol.models.entities.*

/**
 * Intermediary data structure with various optimizations used to generate sync report
 */
class IntermediaryGenSyncReport(val report: SyncReport) {
    //Allocated with predefined size for speed
    val categories = ArrayList<SyncCategory>(INITIAL_LIST_SIZE)
    val chapters = ArrayList<SyncChapter>(INITIAL_LIST_SIZE)
    val histories = ArrayList<SyncHistory>(INITIAL_LIST_SIZE)
    val mangas = ArrayList<SyncManga>(INITIAL_LIST_SIZE)
    val sources = ArrayList<SyncSource>(INITIAL_LIST_SIZE)
    val tracks = ArrayList<SyncTrack>(INITIAL_LIST_SIZE)
    
    private val allEntities = listOf(
            categories,
            chapters,
            histories,
            mangas,
            sources,
            tracks
    )
    
    fun applyToReport() {
        //Allocate arrayList with predefined size for speed
        report.entities = ArrayList(allEntities.map { it.size }.sum())
        
        //Add all entities
        allEntities.forEach {
            report.entities.addAll(it)
        }
    }
    
    companion object {
        private const val INITIAL_LIST_SIZE = 1000
    }
}
