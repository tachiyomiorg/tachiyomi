package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Extension : Serializable {

    var id: Long?

    var isFavorite: Boolean

}
