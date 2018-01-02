package eu.kanade.tachiyomi.data.sync.protocol.snapshot

import eu.kanade.tachiyomi.data.database.models.Category

data class CategorySnapshot(val dbId: Int,
                            val name: String) {
    
    constructor(category: Category): this(category.id!!, category.name)
    
    fun serialize() = "$dbId:$name"
    
    fun matches(category: Category) = dbId == category.id
    
    companion object {
        fun deserialize(string: String)
            = CategorySnapshot(
                string.substringBefore(':').toInt(),
                string.substringAfter(':')
        )
    }
}
