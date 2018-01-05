package eu.kanade.tachiyomi.data.database.resolvers

import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.EntryUpdate
import eu.kanade.tachiyomi.data.database.models.UpdatableField
import eu.kanade.tachiyomi.data.database.queries.updateEntryUpdateQuery
import eu.kanade.tachiyomi.data.database.tables.SyncUpdatesTable
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField

interface EntryUpdateQueries : DbProvider {

    fun getEntryUpdates() = db.get()
            .listOfObjects(EntryUpdate::class.java)
            .withQuery(Query.builder()
                    .table(SyncUpdatesTable.TABLE)
                    .orderBy(SyncUpdatesTable.COL_DATETIME)
                    .build())
            .prepare()

    fun getEntryUpdatesForField(report: SyncReport, field: UpdatableField) = db.get()
            .listOfObjects(EntryUpdate::class.java)
            .withQuery(Query.builder()
                    .table(SyncUpdatesTable.TABLE)
                    .orderBy(SyncUpdatesTable.COL_DATETIME)
                    .where("${SyncUpdatesTable.COL_DATETIME} BETWEEN ? and ? AND ${SyncUpdatesTable.COL_FIELD} = ?")
                    .whereArgs(report.from, report.to, field.id)
                    .build())
            .prepare()
    
    fun getNewerEntryUpdate(id: Long, field: UpdatableField, value: ChangedField<*>) = db.get()
            .`object`(EntryUpdate::class.java)
            .withQuery(Query.builder()
                    .table(SyncUpdatesTable.TABLE)
                    .where("${SyncUpdatesTable.COL_DATETIME} > ? AND ${SyncUpdatesTable.COL_FIELD} = ? AND ${SyncUpdatesTable.COL_UPDATED_ROW} = ?")
                    .whereArgs(value.date, field.id, id)
                    .limit(1)
                    .build())
            .prepare()

    fun insertEntryUpdate(update: EntryUpdate) = db.put().`object`(update).prepare()
    
    fun replaceEntryUpdate(update: EntryUpdate) = db.executeSQL()
            .withQuery(RawQuery.builder()
                    .query(updateEntryUpdateQuery)
                    .args(update.datetime, update.updatedRow, update.field.id)
                    .build())
            .prepare()

    fun deleteEntryUpdate(update: EntryUpdate) = db.delete().`object`(update).prepare()
}
