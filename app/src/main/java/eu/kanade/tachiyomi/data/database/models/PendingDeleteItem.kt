package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface PendingDeleteItem : Serializable {

    var id: Long?

    var chapter_id: Long
}