package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import android.support.v14.preference.MultiSelectListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceGroup
import android.view.View
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.data.source.getLanguages
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import rx.Subscription

class SettingsSourcesFragment : SettingsNestedFragment() {

    companion object {
        const val SOURCE_CHANGE_REQUEST = 120

        fun newInstance(resourcePreference: Int, resourceTitle: Int): SettingsNestedFragment {
            val fragment = SettingsSourcesFragment()
            fragment.setArgs(resourcePreference, resourceTitle)
            return fragment
        }
    }

    val languagesPref by lazy { findPreference("pref_source_languages") as MultiSelectListPreference }
    val sourcesPref by lazy { findPreference("pref_sources") as PreferenceGroup }

    var languagesSubscription: Subscription? = null

    override fun onViewCreated(view: View, savedState: Bundle?) {
        val langs = getLanguages()

        val entryKeys = langs.map { it.code }
        languagesPref.entries = langs.map { it.lang }.toTypedArray()
        languagesPref.entryValues = entryKeys.toTypedArray()
        languagesPref.values = preferences.enabledLanguages().getOrDefault()

        languagesSubscription = preferences.enabledLanguages().asObservable()
                .subscribe { languages ->
                    sourcesPref.removeAll()

                    val enabledSources = settingsActivity.sourceManager.getSources()
                            .filter { it.lang.code in languages }

                    for (source in enabledSources) {
                        if (source.isLoginRequired) {
                            val pref = createSource(source)
                            sourcesPref.addPreference(pref)
                        }
                    }

                    // Hide category if it doesn't have any child
                    sourcesPref.isVisible = sourcesPref.preferenceCount > 0
                }
    }

    override fun onDestroyView() {
        languagesSubscription?.unsubscribe()
        super.onDestroyView()
    }

    fun createSource(source: Source): Preference {
        return LoginPreference(preferenceManager.context).apply {
            key = PreferencesHelper.SOURCE_ACCOUNT_USERNAME + source.id
            title = source.visibleName

            setOnPreferenceClickListener {
                val fragment = SourceLoginDialog.newInstance(source)
                fragment.setTargetFragment(this@SettingsSourcesFragment, SOURCE_CHANGE_REQUEST)
                fragment.show(childFragmentManager, null)
                true
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SOURCE_CHANGE_REQUEST) {
            val pref = findPreference(PreferencesHelper.SOURCE_ACCOUNT_USERNAME + resultCode) as? LoginPreference
            pref?.notifyChanged()
        }
    }

}
