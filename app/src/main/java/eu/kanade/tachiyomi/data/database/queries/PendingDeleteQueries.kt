package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.PendingDeleteItem
import eu.kanade.tachiyomi.data.database.tables.PendingDeleteTable.TABLE
import eu.kanade.tachiyomi.data.database.tables.PendingDeleteTable.COL_CHAPTER_ID

interface PendingDeleteQueries : DbProvider {

    fun getPendingDeleteList() = db.get()
            .listOfObjects(PendingDeleteItem::class.java)
            .withQuery(Query.builder()
                    .table(TABLE)
                    .build()
            )
            .prepare()

    fun getPendingByChapter(chapterId: Long) = db.get()
            .`object`(PendingDeleteItem::class.java)
            .withQuery(Query.builder()
                    .table(TABLE)
                    .where("$COL_CHAPTER_ID = ?")
                    .whereArgs(chapterId)
                    .limit(1)
                    .build())
            .prepare()

    fun insertPending(pd: PendingDeleteItem) = db.put().`object`(pd).prepare()

    fun deletePendingByChapter(chapterId: Long) = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(TABLE)
                    .where("$COL_CHAPTER_ID = ?")
                    .whereArgs(chapterId)
                    .build())
            .prepare()

    fun deleteAllPendingDeleteItems() = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(TABLE)
                    .build())
            .prepare()
}