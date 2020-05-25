package eu.kanade.tachiyomi.data.preference

/**
 * This class stores the values for the preferences in the application.
 */
object PreferenceValues {

    enum class ThemeMode(val value: String) {
        LIGHT("light"),
        DARK("dark"),
        SYSTEM("system"),
    }

    enum class LightThemeVariant(val value: String) {
        DEFAULT("default"),
        BLUE("blue"),
    }

    enum class DarkThemeVariant(val value: String) {
        DEFAULT("default"),
        BLUE("blue"),
        AMOLED("amoled"),
    }

    enum class DisplayMode(val value: Int) {
        COMPACT_GRID(0),
        COMFORTABLE_GRID(1),
        LIST(2),
    }
}
