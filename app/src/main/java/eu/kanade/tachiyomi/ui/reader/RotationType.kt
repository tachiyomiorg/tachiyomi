package eu.kanade.tachiyomi.ui.reader

enum class RotationType(val position: Int, val value: Int) {
    DEFAULT(0, 0x00000000),
    FREE(1, 0x00000008),
    LOCK(2, 0x00000010),
    FORCE_PORTRAIT(3, 0x00000018),
    FORCE_LANDSCAPE(4, 0x00000020);

    companion object {
        const val MASK = 0x00000038

        fun valueAtPosition(position: Int?) = values().find { it.position == position }
            ?: DEFAULT

        fun valueOf(value: Int?) = values().find { it.value == value }
            ?: DEFAULT
    }
}
