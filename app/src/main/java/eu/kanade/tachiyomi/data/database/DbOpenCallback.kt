package eu.kanade.tachiyomi.data.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import eu.kanade.tachiyomi.data.database.models.Updatable
import eu.kanade.tachiyomi.data.database.models.UpdateTarget
import eu.kanade.tachiyomi.data.database.queries.getGenerateAllEntryUpdatesQueries
import eu.kanade.tachiyomi.data.database.tables.*

class DbOpenCallback : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_NAME = "tachiyomi.db"

        /**
         * Version of the database.
         */
        const val DATABASE_VERSION = 9
    }

    override fun onCreate(db: SupportSQLiteDatabase) = with(db) {
        execSQL(MangaTable.createTableQuery)
        execSQL(ChapterTable.createTableQuery)
        execSQL(TrackTable.createTableQuery)
        execSQL(CategoryTable.createTableQuery)
        execSQL(MangaCategoryTable.createTableQuery)
        execSQL(HistoryTable.createTableQuery)
        execSQL(SyncUpdatesTable.createTableQuery)

        // DB indexes
        execSQL(MangaTable.createUrlIndexQuery)
        execSQL(MangaTable.createLibraryIndexQuery)
        execSQL(ChapterTable.createMangaIdIndexQuery)
        execSQL(ChapterTable.createUnreadChaptersIndexQuery)
        execSQL(HistoryTable.createChapterIdIndexQuery)
    
        // Gen and apply triggers for sync
        UpdateTarget.registeredObjects.flatMap(Updatable::getTriggers).forEach {
            execSQL(it)
        }
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(ChapterTable.sourceOrderUpdateQuery)

            // Fix kissmanga covers after supporting cloudflare
            db.execSQL("""UPDATE mangas SET thumbnail_url =
                    REPLACE(thumbnail_url, '93.174.95.110', 'kissmanga.com') WHERE source = 4""")
        }
        if (oldVersion < 3) {
            // Initialize history tables
            db.execSQL(HistoryTable.createTableQuery)
            db.execSQL(HistoryTable.createChapterIdIndexQuery)
        }
        if (oldVersion < 4) {
            db.execSQL(ChapterTable.bookmarkUpdateQuery)
        }
        if (oldVersion < 5) {
            db.execSQL(ChapterTable.addScanlator)
        }
        if (oldVersion < 6) {
            db.execSQL(TrackTable.addTrackingUrl)
        }
        if (oldVersion < 7) {
            db.execSQL(TrackTable.addLibraryId)
        }
        if (oldVersion < 8) {
            db.execSQL("DROP INDEX IF EXISTS mangas_favorite_index")
            db.execSQL(MangaTable.createLibraryIndexQuery)
            db.execSQL(ChapterTable.createUnreadChaptersIndexQuery)
        }
        if (oldVersion < 9) {
            // Create sync updates table
            db.execSQL(SyncUpdatesTable.createTableQuery)

            // Add missing entry updates
            UpdateTarget.registeredObjects.forEach {
                getGenerateAllEntryUpdatesQueries(it).forEach {
                    db.execSQL(it)
                }
            }

            // Gen and apply triggers for sync
            UpdateTarget.registeredObjects.flatMap(Updatable::getTriggers).forEach {
                db.execSQL(it)
            }
        }
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

}
