package eu.kanade.tachiyomi.data.database.models

class PendingDeleteItemImpl(override var chapter_id: Long) : PendingDeleteItem {
    override var id: Long? = null
}