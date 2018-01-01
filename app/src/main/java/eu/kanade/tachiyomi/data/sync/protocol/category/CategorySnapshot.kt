package eu.kanade.tachiyomi.data.sync.protocol.category

import eu.kanade.tachiyomi.data.database.models.Category

data class CategorySnapshot(val dbId: Int,
                            val name: String) {
    
    constructor(category: Category): this(category.id!!, category.name)
    
    fun serialize() = "$dbId:$name"
    
    companion object {
        fun deserialize(string: String)
            = CategorySnapshot(
                string.substringBefore(':').toInt(),
                string.substringAfter(':')
        )
    }
}
