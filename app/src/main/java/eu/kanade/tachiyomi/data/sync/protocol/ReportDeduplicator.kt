package eu.kanade.tachiyomi.data.sync.protocol

import eu.kanade.tachiyomi.data.sync.protocol.models.SyncCategory
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncChapter
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncManga
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport

class ReportDeduplicator {
    /**
     * Remove any entries inside both `main` and `subset` from `subset`
     */
    fun dedup(main: SyncReport, subset: SyncReport) {

    }

    private fun dedupManga(main: SyncReport, subset: SyncReport) {
        //Clear unused fields
        subset.findEntities<SyncManga>().forEach { subManga ->
            val mainManga = main.findEntity<SyncManga> { mainManga ->
                subManga.url == mainManga.url
                        && subManga.source.resolve(subset).id == mainManga.source.resolve(main).id
            }

            mainManga?.let {
                if(mainManga.favorite?.value == subManga.favorite?.value)
                    mainManga.favorite = null

                if(mainManga.viewer?.value == subManga.viewer?.value)
                    mainManga.viewer = null

                if(mainManga.chapterFlags?.value == subManga.chapterFlags?.value)
                    mainManga.chapterFlags = null
            }
        }

        //Clear unused objects
        subset.findEntities<SyncManga>().forEachIndexed { index, manga ->
            if(!subset.findEntities<SyncChapter>().any { it.manga.targetId == manga.syncId }
                    && !subset.findEntities<SyncCategory>().flatMap {
                (it.addedManga?.value ?: emptyList()) + (it.deletedManga?.value ?: emptyList())
            }.any { it.targetId == manga.syncId }) {
                subset.entities.remove(manga)
            }
        }
    }

    private fun dedupChapter(chapter: SyncReport) {

    }
}