package eu.kanade.tachiyomi.data.database

import android.content.Context
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.database.queries.*

/**
 * This class provides operations to manage the database through its interfaces.
 */
open class DatabaseHelper(context: Context)
: MangaQueries, ChapterQueries, MangaSyncQueries, CategoryQueries, MangaCategoryQueries, HistoryQueries {

    override val db = DefaultStorIOSQLite.builder()
            .sqliteOpenHelper(DbOpenHelper(context))
            .addTypeMapping(Manga::class.java, MangaSQLiteTypeMapping())
            .addTypeMapping(Chapter::class.java, ChapterSQLiteTypeMapping())
            .addTypeMapping(MangaSync::class.java, MangaSyncSQLiteTypeMapping())
            .addTypeMapping(Category::class.java, CategorySQLiteTypeMapping())
            .addTypeMapping(MangaCategory::class.java, MangaCategorySQLiteTypeMapping())
            .addTypeMapping(History::class.java, HistorySQLiteTypeMapping())
            .build()

    inline fun inTransaction(block: () -> Unit) = db.inTransaction(block)

}
