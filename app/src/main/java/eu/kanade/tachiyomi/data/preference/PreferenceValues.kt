package eu.kanade.tachiyomi.data.preference

/**
 * This class stores the values for the preferences in the application.
 */
object PreferenceValues {

    const val THEME_MODE_LIGHT = "light"
    const val THEME_MODE_DARK = "dark"
    const val THEME_MODE_SYSTEM = "system"

    const val THEME_LIGHT_DEFAULT = "default"
    const val THEME_LIGHT_BLUE = "blue"

    const val THEME_DARK_DEFAULT = "default"
    const val THEME_DARK_BLUE = "blue"
    const val THEME_DARK_AMOLED = "amoled"

    enum class DisplayMode(val value: Int) {
        COMPACT_GRID(0),
        COMFORTABLE_GRID(1),
        LIST(2),
    }
}
