package eu.kanade.tachiyomi.ui.browse.source

import android.graphics.drawable.Drawable
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.minusAssign
import eu.kanade.tachiyomi.data.preference.plusAssign
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.getPreferenceKey
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.switchPreferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

class SourceFilterController : SettingsController() {

    private val onlineSources by lazy { Injekt.get<SourceManager>().getOnlineSources() }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.label_sources

        // Get the list of active language codes.
        val activeLangsCodes = preferences.enabledLanguages().get()

        // Get a map of sources grouped by language.
        val sourcesByLang = onlineSources.groupByTo(TreeMap(), { it.lang })

        // Order first by active languages, then inactive ones
        val orderedLangs = sourcesByLang.keys.sortedWith(
            compareBy(
                { it !in activeLangsCodes },
                { LocaleHelper.getSourceDisplayName(it, context) }
            )
        )

        orderedLangs.forEach { lang ->
            val sources = sourcesByLang[lang].orEmpty().sortedBy { it.name.lowercase() }

            // Create a preference group and set initial state and change listener
            switchPreferenceCategory {
                this@apply.addPreference(this)
                title = LocaleHelper.getSourceDisplayName(lang, context)
                isPersistent = false
                if (lang in activeLangsCodes) {
                    setChecked(true)
                    addLanguageSources(this, sources)
                }

                onChange { newValue ->
                    val checked = newValue as Boolean
                    if (!checked) {
                        preferences.enabledLanguages() -= lang
                        removeAll()
                    } else {
                        preferences.enabledLanguages() += lang
                        addLanguageSources(this, sources)
                    }
                    true
                }
            }
        }
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(null)
    }

    /**
     * Adds the source list for the given group (language).
     *
     * @param group the language category.
     */
    private fun addLanguageSources(group: PreferenceGroup, sources: List<HttpSource>) {
        val disabledSourceIds = preferences.disabledSources().get()

        sources
            .sortedBy { it.id.toString() in disabledSourceIds }
            .map { source ->
                CheckBoxPreference(group.context).apply {
                    val id = source.id.toString()
                    title = source.name
                    key = source.getPreferenceKey()
                    isPersistent = false
                    isChecked = id !in disabledSourceIds

                    val sourceIcon = source.icon()
                    if (sourceIcon != null) {
                        icon = sourceIcon
                    }

                    onChange { newValue ->
                        val checked = newValue as Boolean
                        if (checked) {
                            preferences.disabledSources() -= id
                        } else {
                            preferences.disabledSources() += id
                        }
                        true
                    }
                }
            }
            .forEach { group.addPreference(it) }
    }
}
