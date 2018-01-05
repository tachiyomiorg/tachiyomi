package eu.kanade.tachiyomi.data.database.models

interface EntryUpdate {
    var id: Long?

    var updatedRow: Long

    var datetime: Long //Millis since epoch in UTC

    var field: UpdatableField //The updated field
    
    companion object {
        fun create(updatedRow: Long,
                   datetime: Long,
                   field: UpdatableField) = EntryUpdateImpl().apply {
            this.updatedRow = updatedRow
            this.datetime = datetime
            this.field = field
        }
    }
}