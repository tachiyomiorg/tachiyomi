package eu.kanade.tachiyomi.data.database.models

class CategoryImpl : Category {

    override var id: Int? = null

    override lateinit var name: String

    override var order: Int = 0

    override var flags: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val category = other as Category
        return name == category.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
