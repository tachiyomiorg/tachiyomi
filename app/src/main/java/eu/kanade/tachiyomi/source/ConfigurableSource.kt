package eu.kanade.tachiyomi.source

import androidx.preference.PreferenceScreen

interface ConfigurableSource : Source {

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
