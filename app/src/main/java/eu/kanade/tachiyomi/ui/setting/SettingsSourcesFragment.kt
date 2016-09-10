package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceGroup
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.data.source.online.LoginSource
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import uy.kohesive.injekt.injectLazy

class SettingsSourcesFragment : SettingsFragment() {

    companion object {
        const val SOURCE_CHANGE_REQUEST = 120

        fun newInstance(rootKey: String?): SettingsSourcesFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsSourcesFragment().apply { arguments = args }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    private val sourceManager: SourceManager by injectLazy()

    val sourcesPref by lazy { findPreference("pref_login_sources") as PreferenceGroup }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        subscriptions += preferences.enabledLanguages().asObservable()
                .subscribe { languages ->
                    sourcesPref.removeAll()

                    val loginSources = sourceManager.getOnlineSources()
                            .filter { it.lang.code in languages }
                            .filterIsInstance(LoginSource::class.java)

                    for (source in loginSources) {
                        val pref = createLoginSourceEntry(source)
                        sourcesPref.addPreference(pref)
                    }

                    // Hide category if it doesn't have any child
                    sourcesPref.isVisible = sourcesPref.preferenceCount > 0
                }
    }

    fun createLoginSourceEntry(source: Source): Preference {
        return LoginPreference(preferenceManager.context).apply {
            key = preferences.keys.sourceUsername(source.id)
            title = source.toString()

            setOnPreferenceClickListener {
                val fragment = SourceLoginDialog.newInstance(source)
                fragment.setTargetFragment(this@SettingsSourcesFragment, SOURCE_CHANGE_REQUEST)
                fragment.show(fragmentManager, null)
                true
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SOURCE_CHANGE_REQUEST) {
            val pref = findPreference(preferences.keys.sourceUsername(resultCode)) as? LoginPreference
            pref?.notifyChanged()
        }
    }

}
