package eu.kanade.tachiyomi.data.preference

/**
 * This class stores the values for the preferences in the application.
 */
object PreferenceValues {

    // Keys are lowercase to match legacy string values
    enum class ThemeMode {
        light,
        dark,
        system,
    }

    // Keys are lowercase to match legacy string values
    enum class LightThemeVariant {
        default,
        blue,
    }

    // Keys are lowercase to match legacy string values
    enum class DarkThemeVariant {
        default,
        blue,
        amoled,
    }

    enum class DisplayMode {
        COMPACT_GRID,
        COMFORTABLE_GRID,
        LIST,
    }

    enum class TappingInvertMode(val shouldInvertHorizontal: Boolean = false, val shouldInvertVertical: Boolean = false) {
        NONE,
        HORIZONTAL(shouldInvertHorizontal = true),
        VERTICAL(shouldInvertVertical = true),
        BOTH(shouldInvertHorizontal = true, shouldInvertVertical = true)
    }
}
