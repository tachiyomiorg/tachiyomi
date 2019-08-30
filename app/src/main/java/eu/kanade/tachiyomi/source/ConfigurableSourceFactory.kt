package eu.kanade.tachiyomi.source

import android.support.v7.preference.PreferenceScreen

/**
 * A factory for creating sources at runtime.
 */
interface ConfigurableSourceFactory: SourceFactory {
    /**
     * Create a new copy of the sources
     * @return The created sources
     */
    fun setupPreferenceScreen(screen: PreferenceScreen)
}
