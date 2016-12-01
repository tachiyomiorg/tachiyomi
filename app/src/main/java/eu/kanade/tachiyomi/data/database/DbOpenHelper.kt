package eu.kanade.tachiyomi.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import eu.kanade.tachiyomi.data.database.tables.*
import java.util.*

class DbOpenHelper(context: Context)
: SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_NAME = "tachiyomi.db"

        /**
         * Version of the database.
         */
        const val DATABASE_VERSION = 5
    }

    override fun onCreate(db: SQLiteDatabase) = with(db) {
        execSQL(MangaTable.createTableQuery)
        execSQL(ChapterTable.createTableQuery)
        execSQL(MangaSyncTable.createTableQuery)
        execSQL(CategoryTable.createTableQuery)
        execSQL(MangaCategoryTable.createTableQuery)
        execSQL(HistoryTable.createTableQuery)

        // DB indexes
        execSQL(MangaTable.createUrlIndexQuery)
        execSQL(MangaTable.createFavoriteIndexQuery)
        execSQL(ChapterTable.createMangaIdIndexQuery)
        execSQL(HistoryTable.createChapterIdIndexQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
            upgradeCategories(db)
        }
    }

    fun upgradeCategories(db: SQLiteDatabase) {
        //TODO This is really messy, is there a better way to do this?
        try {
            //HACK to end the invisible transaction that SQLiteOpenHelper has when upgrading a DB
            //We can't enable/disable pragmas with a transaction open
            db.setTransactionSuccessful()
            db.endTransaction()
            //Don't delete the manga categories while we migrate
            db.setForeignKeyConstraintsEnabled(false)
            //Old category model
            data class OldCategory(val id: Int,
                                   val name: String,
                                   val order: Int,
                                   val flags: Int) {
                val dupIds = ArrayList<Int>()
            }

            val oldCategories = ArrayList<OldCategory>()
            //Read categories
            db.rawQuery("SELECT * FROM categories", emptyArray()).use {
                it.moveToNext()
                loop@ while (!it.isAfterLast) {
                    val catId = it.getInt(it.getColumnIndex("_id"))
                    val catName = it.getString(it.getColumnIndex("name"))
                    val catSort = it.getInt(it.getColumnIndex("sort"))
                    val catFlags = it.getInt(it.getColumnIndex("flags"))
                    it.moveToNext()
                    //Find anything with the same name and if we do, merge it
                    for (oldCategory in oldCategories) {
                        if (oldCategory.name.equals(catName, true)) {
                            oldCategory.dupIds += catId
                            continue@loop
                        }
                    }
                    //Nothing with the same name, add as new category
                    oldCategories += OldCategory(catId,
                            catName,
                            catSort,
                            catFlags)
                }
            }
            //Add new temp table
            db.execSQL("""CREATE TABLE categories_tmp(
                _id INTEGER NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                sort INTEGER NOT NULL,
                flags INTEGER NOT NULL,
                UNIQUE (name COLLATE NOCASE)
            )""")
            //Insert categories into new table
            for ((id, name, order, flags) in oldCategories) {
                db.execSQL("INSERT INTO categories_tmp VALUES (?,?,?,?)", arrayOf(id.toString(),
                        name,
                        order.toString(),
                        flags.toString()))
            }
            //Drop old table
            db.execSQL("DROP TABLE categories")
            //Rename temp table to original table name
            db.execSQL("ALTER TABLE categories_tmp RENAME TO categories")
            //Rename duped category references
            oldCategories
                    .filter { it.dupIds.size > 0 }
                    .forEach { oldCategory ->
                        oldCategory.dupIds.forEach {
                            db.execSQL("""UPDATE mangas_categories
                        SET category_id=?
                        WHERE category_id=?""", arrayOf(oldCategory.id, it))
                        }
                    }
            //Enable foreign constraints again
            db.setForeignKeyConstraintsEnabled(true)
            //Open the invisible transaction again
            db.beginTransaction()
        } catch(e: Exception) {
            timber.log.Timber.e(e, "Database upgrade failure")
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

}
