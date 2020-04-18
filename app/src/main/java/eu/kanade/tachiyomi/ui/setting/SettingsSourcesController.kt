package eu.kanade.tachiyomi.ui.setting

import android.graphics.drawable.Drawable
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.switchPreferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
import java.util.TreeMap
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsSourcesController : SettingsController() {

    private val onlineSources by lazy { Injekt.get<SourceManager>().getOnlineSources() }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.action_filter

        // Get the list of active language codes.
        val activeLangsCodes = preferences.enabledLanguages().get()

        // Get a map of sources grouped by language.
        val sourcesByLang = onlineSources.groupByTo(TreeMap(), { it.lang })

        // Order first by active languages, then inactive ones
        val orderedLangs = sourcesByLang.keys.filter { it in activeLangsCodes } +
                sourcesByLang.keys.filterNot { it in activeLangsCodes }

        orderedLangs.forEach { lang ->
            val sources = sourcesByLang[lang].orEmpty().sortedBy { it.name }

            // Create a preference group and set initial state and change listener
            switchPreferenceCategory {
                preferenceScreen.addPreference(this)
                title = LocaleHelper.getSourceDisplayName(lang, context)
                isPersistent = false
                if (lang in activeLangsCodes) {
                    setChecked(true)
                    addLanguageSources(this, sources)
                }

                onChange { newValue ->
                    val checked = newValue as Boolean
                    val current = preferences.enabledLanguages().get()
                    if (!checked) {
                        preferences.enabledLanguages().set(current - lang)
                        removeAll()
                    } else {
                        preferences.enabledLanguages().set(current + lang)
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
        val hiddenCatalogues = preferences.hiddenCatalogues().get()

        sources.forEach { source ->
            val sourcePreference = CheckBoxPreference(group.context).apply {
                val id = source.id.toString()
                title = source.name
                key = getSourceKey(source.id)
                isPersistent = false
                isChecked = id !in hiddenCatalogues

                val sourceIcon = source.icon()
                if (sourceIcon != null) {
                    icon = sourceIcon
                }

                onChange { newValue ->
                    val checked = newValue as Boolean
                    val current = preferences.hiddenCatalogues().get()

                    preferences.hiddenCatalogues().set(if (checked)
                        current - id
                    else
                        current + id)

                    true
                }
            }

            group.addPreference(sourcePreference)
        }
    }

    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }
}
