package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.preference.Preference
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import eu.kanade.tachiyomi.R
import net.xpece.android.support.preference.PreferenceScreenNavigationStrategy
import rx.subscriptions.CompositeSubscription

open class SettingsFragment : XpPreferenceFragment() {

    companion object {
        fun newInstance(rootKey: String?): SettingsFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsFragment().apply { arguments = args }
        }
    }

    lateinit var subscriptions: CompositeSubscription

    override final fun onCreatePreferences2(savedState: Bundle?, rootKey: String?) {
        subscriptions = CompositeSubscription()

        addPreferencesFromResource(R.xml.pref_general)
        addPreferencesFromResource(R.xml.pref_reader)
        addPreferencesFromResource(R.xml.pref_downloads)
        addPreferencesFromResource(R.xml.pref_sources)
        addPreferencesFromResource(R.xml.pref_tracking)
        addPreferencesFromResource(R.xml.pref_advanced)
        addPreferencesFromResource(R.xml.pref_about)

        // Setup root preference title.
        preferenceScreen.title = activity.title

        PreferenceScreenNavigationStrategy.ReplaceFragment.onCreatePreferences(this, rootKey)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)
        listView.isFocusable = false
    }

    override fun onStart() {
        super.onStart()
        activity.title = preferenceScreen.title
    }

    override fun onDestroyView() {
        subscriptions.unsubscribe()
        super.onDestroyView()
    }

    protected inline fun <reified T : Preference> bindPref(resId: Int): Lazy<T> {
        return lazy { findPreference(getString(resId)) as T }
    }

}