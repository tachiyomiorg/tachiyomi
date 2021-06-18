package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Category : Serializable {

    var id: Int?

    var name: String

    var order: Int

    var flags: Int

    private fun setFlags(flag: Int, mask: Int) {
        flags = flags and mask.inv() or (flag and mask)
    }

    var displayMode: Int
        get() = flags and MASK
        set(mode) = setFlags(mode, MASK)

    companion object {

        const val COMPACT_GRID = 0b00000000
        const val COMFORTABLE_GRID = 0b00000001
        const val LIST = 0b00000010
        const val MASK = 0b00000011

        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(): Category = create("Default").apply { id = 0 }
    }
}
