package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.data.database.TriggerGenerator
import eu.kanade.tachiyomi.data.database.tables.*

object UpdateTarget {
    val registeredObjects = listOf(
            Manga,
            Chapter,
            History,
            Category,
            Track
    )

    fun find(id: Int) = registeredObjects.flatMap {
        it.fields
    }.find { it.id == id }

    // Object definitions follow

    object Manga : Updatable() {
        override val id = 0

        override val tableName = MangaTable.TABLE

        override val idColumn = MangaTable.COL_ID

        val favorite = field(0, MangaTable.COL_FAVORITE, false)
        val viewer = field(1, MangaTable.COL_VIEWER, 0)
        val chapterFlags = field(2, MangaTable.COL_CHAPTER_FLAGS, 0)
    }
    object Chapter: Updatable() {
        override val id = 1

        override val tableName = ChapterTable.TABLE

        override val idColumn = ChapterTable.COL_ID

        val read = field(3, ChapterTable.COL_READ, false)
        val bookmark = field(4, ChapterTable.COL_BOOKMARK, false)
        val lastPageRead = field(5, ChapterTable.COL_LAST_PAGE_READ, 0)
    }
    object History: Updatable() {
        override val id = 2

        override val tableName = HistoryTable.TABLE

        override val idColumn = HistoryTable.COL_ID

        val lastRead = field(6, HistoryTable.COL_LAST_READ, 0)
    }
    object Category: Updatable() {
        override val id = 3
        
        override val tableName = CategoryTable.TABLE
    
        override val idColumn = CategoryTable.COL_ID
    
        val flags = field(7, CategoryTable.COL_FLAGS, 0)
    }
    object Track: Updatable() {
        override val id = 4
    
        override val tableName = TrackTable.TABLE
    
        override val idColumn = TrackTable.COL_ID
        
        val remoteId = field(8, TrackTable.COL_REMOTE_ID, 0)
        var title = field(9, TrackTable.COL_TITLE, "")
        var lastChapterRead = field(10, TrackTable.COL_LAST_CHAPTER_READ, 0)
        var totalChapters = field(11, TrackTable.COL_TOTAL_CHAPTERS, 0)
        var score = field(12, TrackTable.COL_SCORE, 0f)
        var status = field(13, TrackTable.COL_STATUS, 0)
    }
}

abstract class Updatable {
    abstract val id: Int

    abstract val tableName: String

    abstract val idColumn: String

    val fields = mutableListOf<UpdatableField>()

    fun field(id: Int, field: String, defValue: Any)
            = UpdatableField(this, id, field, defValue).apply {
        fields.add(this)
    }

    fun getTriggers() = fields.flatMap {
        TriggerGenerator().genTriggers(it)
    }
}
