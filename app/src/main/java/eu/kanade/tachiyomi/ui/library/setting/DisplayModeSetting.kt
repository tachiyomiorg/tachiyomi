package eu.kanade.tachiyomi.ui.library.setting

enum class DisplayModeSetting(val flag: Int) {
    COMPACT_GRID(0b00000000),
    COMFORTABLE_GRID(0b00000001),
    LIST(0b00000010);

    companion object {
        const val MASK = 0b00000011

        fun fromFlag(flag: Int?): DisplayModeSetting {
            return values()
                .find { mode -> mode.flag == flag } ?: COMPACT_GRID
        }
    }
}
