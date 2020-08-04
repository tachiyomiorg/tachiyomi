package eu.kanade.tachiyomi.ui.reader

enum class ReadingMode(val position: Int, val value: Int) {
    DEFAULT(0, 0x00000000),
    LEFT_TO_RIGHT(1, 0x00000001),
    RIGHT_TO_LEFT(2, 0x00000002),
    VERTICAL(3, 0x00000003),
    WEBTOON(4, 0x00000004),
    CONTINOUS_VERTICAL(5, 0x00000005);

    companion object {
        const val MASK = 0x00000007

        fun valueAtPosition(position: Int?) = values().find { it.position == position }
            ?: DEFAULT

        fun valueOf(value: Int?) = values().find { it.value == value }
            ?: DEFAULT
    }
}
