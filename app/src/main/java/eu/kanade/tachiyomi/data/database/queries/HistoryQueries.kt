package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.tables.HistoryTable
import java.util.*

interface HistoryQueries : DbProvider {

    fun insertHistory(history: History) = db.put().`object`(history).prepare()

    fun getRecentManga(date: Date) = db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(RawQuery.builder()
                    .query(getRecentMangasQuery())
                    .args(date.time)
                    .observesTables(HistoryTable.TABLE)
                    .build())
            .prepare()

    fun getHistoryByMangaId(id: Long) = db.get()
            .`object`(History::class.java)
            .withQuery(Query.builder()
                    .table(HistoryTable.TABLE)
                    .where("${HistoryTable.COL_MANGA_ID} = ?")
                    .whereArgs(id)
                    .build())
            .prepare()

}