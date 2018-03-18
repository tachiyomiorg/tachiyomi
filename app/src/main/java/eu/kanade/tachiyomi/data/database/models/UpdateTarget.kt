package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.data.database.TriggerGenerator
import eu.kanade.tachiyomi.data.database.tables.*

/**
 * Maps all properties of objects that can be changed
 * in the database to unique IDs
 */
object UpdateTarget {
    /**
     * Objects that can have their properties changed
     */
    val registeredObjects = listOf(
            Manga,
            Chapter,
            History,
            Category,
            Track
    )

    /**
     * All possible fields
     */
    val fields = registeredObjects.flatMap { it.fields }.sortedBy { it.id }

    /**
     * Find a property by it's ID
     */
    fun find(id: Int) = fields[fields.binarySearchBy(id) { it.id }]

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
        val title = field(9, TrackTable.COL_TITLE, "")
        val lastChapterRead = field(10, TrackTable.COL_LAST_CHAPTER_READ, 0)
        val totalChapters = field(11, TrackTable.COL_TOTAL_CHAPTERS, 0)
        val score = field(12, TrackTable.COL_SCORE, 0f)
        val status = field(13, TrackTable.COL_STATUS, 0)
        val trackingUrl = field(14, TrackTable.COL_TRACKING_URL, "")
    }
}

/**
 * An object type that can have it's properties changed in the DB
 */
abstract class Updatable {
    /**
     * The unique ID for this object type
     * Each object type MUST have a different ID
     * This ID MUST NOT change between app versions
     */
    abstract val id: Int

    /**
     * The name of the table that holds this object type in the DB
     */
    abstract val tableName: String

    /**
     * The column used to differentiate between instances of this object type in the DB
     */
    abstract val idColumn: String

    /**
     * A list of the mutable properties of the object type
     */
    val fields = mutableListOf<UpdatableField>()
    
    /**
     * Define a mutable property for this object type
     *
     * @param id A unique ID for this property
     *   Each field MUST have a different ID, even if they belong to different object types!
     *   This ID MUST NOT change between app versions
     * @param field The name of the column that holds this property in the DB
     * @param defValue The default value of this property. Properties of newly inserted objects that match
     *   this default value will not be recorded as 'changed'.
     */
    fun field(id: Int, field: String, defValue: Any)
            = UpdatableField(this, id, field, defValue).apply {
        fields.add(this)
    }
    
    /**
     * Generate the triggers required to listen for property changes for this object
     */
    fun getTriggers() = fields.flatMap {
        TriggerGenerator().genTriggers(it)
    }
}
