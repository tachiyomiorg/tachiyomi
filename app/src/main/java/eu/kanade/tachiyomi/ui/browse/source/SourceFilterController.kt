package eu.kanade.tachiyomi.ui.browse.source

import android.graphics.drawable.Drawable
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
import java.util.TreeMap
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourceFilterController : SettingsController() {

    private val onlineSources by lazy { Injekt.get<SourceManager>().getOnlineSources() }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.label_sources

        // Get the list of active language codes.
        val activeLangsCodes = preferences.enabledLanguages().get()

        // Get a map of sources grouped by language.
        val sourcesByLang = onlineSources.groupByTo(TreeMap(), { it.lang })

        // Order first by active languages, then inactive ones
        val orderedLangs = sourcesByLang.keys.sortedWith(compareBy({ it !in activeLangsCodes }, { LocaleHelper.getSourceDisplayName(it, context) }))

        orderedLangs.forEach { lang ->
            switchPreference {
                preferenceScreen.addPreference(this)
                title = LocaleHelper.getSourceDisplayName(lang, context)
                isPersistent = false
                isChecked = lang in activeLangsCodes

                onChange { newValue ->
                    val checked = newValue as Boolean
                    val current = preferences.enabledLanguages().get()
                    preferences.enabledLanguages().set(
                        if (!checked) {
                            current - lang
                        } else {
                            current + lang
                        }
                    )
                    true
                }
            }
        }
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(null)
    }
}
