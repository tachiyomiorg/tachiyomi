package eu.kanade.tachiyomi.data.database.models

/**
 * Represents the change of a single property
 */
interface EntryUpdate {
    var id: Long?
    
    /**
     * The row where this property changed in
     */
    var updatedRow: Long
    
    /**
     * Millis since epoch in UTC representing when this property was changed
     */
    var datetime: Long
    
    /**
     * The field that was changed
     */
    var field: UpdatableField
    
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