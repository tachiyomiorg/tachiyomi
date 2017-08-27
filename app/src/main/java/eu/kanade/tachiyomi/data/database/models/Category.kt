package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Category : Serializable {

    var id: Int?

    var name: String

    var order: Int

    var flags: Int

    val nameLower: String
        get() = name.toLowerCase()

    companion object {

        const val ALL_CATEGORY_ID = 0

        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(): Category = create("All").apply { id = ALL_CATEGORY_ID }
    }

}