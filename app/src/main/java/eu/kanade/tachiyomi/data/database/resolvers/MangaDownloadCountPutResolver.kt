package eu.kanade.tachiyomi.data.database.resolvers

/**
 * Created by Carlos on 11/26/2017.
 */

import android.content.ContentValues
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.tables.MangaTable
import timber.log.Timber

class MangaDownloadCountPutResolver : PutResolver<Manga>() {

    override fun performPut(db: StorIOSQLite, manga: Manga) = db.inTransactionReturn {
        val updateQuery = mapToUpdateQuery(manga)
        val contentValues = mapToContentValues(manga)

        val numberOfRowsUpdated = db.lowLevel().update(updateQuery, contentValues)
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }

    fun mapToUpdateQuery(manga: Manga) = UpdateQuery.builder()
            .table(MangaTable.TABLE)
            .where("${MangaTable.COL_ID} = ?")
            .whereArgs(manga.id)
            .build()

    fun mapToContentValues(manga: Manga) = ContentValues(1).apply {
        Timber.d("manga download count: %s", manga.download_count)
        put(MangaTable.COL_DOWNLOAD_COUNT, manga.download_count)
    }

}