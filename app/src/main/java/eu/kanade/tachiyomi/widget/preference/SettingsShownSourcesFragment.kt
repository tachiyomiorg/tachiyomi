package eu.kanade.tachiyomi.widget.preference

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.getLanguages
import eu.kanade.tachiyomi.ui.setting.SettingsFragment
import net.xpece.android.support.preference.CheckBoxPreference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsShownSourcesFragment : SettingsFragment() {

    companion object {
        fun newInstance(rootKey: String): SettingsShownSourcesFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsShownSourcesFragment().apply { arguments = args }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    private val onlineSources by lazy { Injekt.get<SourceManager>().getOnlineSources() }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(null)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        // Remove dummy preference
        preferenceScreen.removeAll()

        // Get the list of active language codes.
        val activeLangsCodes = preferences.enabledLanguages().getOrDefault()

        // Get the list of languages ordered by name.
        val langs = getLanguages().sortedBy { it.lang }

        // Order first by active languages, then inactive ones
        val orderedLangs = langs.filter { it.code in activeLangsCodes } +
                langs.filterNot { it.code in activeLangsCodes }

        orderedLangs.forEach { lang ->
            // Create a preference group and set initial state and change listener
            SwitchPreferenceCategory(context).apply {
                preferenceScreen.addPreference(this)
                title = lang.lang
                isPersistent = false
                if (lang.code in activeLangsCodes) {
                    setChecked(true)
                    addLanguageSources(this)
                }

                setOnPreferenceChangeListener { preference, any ->
                    val checked = any as Boolean
                    val current = preferences.enabledLanguages().getOrDefault()
                    if (!checked) {
                        preferences.enabledLanguages().set(current - lang.code)
                        removeAll()
                    } else {
                        preferences.enabledLanguages().set(current + lang.code)
                        addLanguageSources(this)
                    }
                    true
                }
            }
        }
    }

    /**
     * Adds the source list for the given group (language).
     *
     * @param group the language category.
     */
    private fun addLanguageSources(group: SwitchPreferenceCategory) {
        val sources = onlineSources.filter { it.lang.lang == group.title }.sortedBy { it.name }
        val hiddenCatalogues = preferences.hiddenCatalogues().getOrDefault()

        sources.forEach { source ->
            val sourcePreference = CheckBoxPreference(context, null).apply {
                layoutResource = R.layout.pref_item_catalog_selection
                title = source.name
                isPersistent = false
                isChecked = source.id.toString() !in hiddenCatalogues

                setOnPreferenceChangeListener { preference, any ->
                    val checked = any as Boolean
                    val current = preferences.hiddenCatalogues().getOrDefault()
                    val id = source.id.toString()

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

}