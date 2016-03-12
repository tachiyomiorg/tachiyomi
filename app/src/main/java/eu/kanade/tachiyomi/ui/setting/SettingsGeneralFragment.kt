package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.TaskStackBuilder
import android.support.v7.preference.Preference
import android.view.View
import android.widget.Toast
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateAlarm
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.ToastUtil
import eu.kanade.tachiyomi.widget.preference.IntListPreference
import eu.kanade.tachiyomi.widget.preference.LibraryColumnsDialog
import eu.kanade.tachiyomi.widget.preference.SimpleDialogPreference
import rx.Observable
import rx.Subscription

class SettingsGeneralFragment : SettingsNestedFragment() {

    companion object {
        fun newInstance(resourcePreference: Int, resourceTitle: Int): SettingsGeneralFragment {
            val fragment = SettingsGeneralFragment();
            fragment.setArgs(resourcePreference, resourceTitle);
            return fragment;
        }
    }

    val columnsPreference by lazy {
        findPreference(getString(R.string.pref_library_columns_dialog_key)) as SimpleDialogPreference
    }

    val updateInterval by lazy {
        findPreference(getString(R.string.pref_library_update_interval_key)) as IntListPreference
    }

    val themePreference by lazy {
        findPreference(getString(R.string.pref_theme_key)) as IntListPreference
    }

    var columnsSubscription: Subscription? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateInterval.setOnPreferenceChangeListener { preference, newValue ->
            LibraryUpdateAlarm.startAlarm(activity, (newValue as String).toInt())
            true
        }

        themePreference.setOnPreferenceChangeListener { preference, newValue ->
            // Rebuild activity's to apply themes.
            TaskStackBuilder.create(activity)
                    .addNextIntent(Intent(activity, MainActivity::class.java))
                    .addNextIntent(activity.intent)
                    .startActivities()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        columnsSubscription = Observable.combineLatest(preferences.portraitColumns().asObservable(),
                preferences.landscapeColumns().asObservable(),
                { portraitColumns, landscapeColumns -> Pair(portraitColumns, landscapeColumns) })
                .subscribe { updateColumnsSummary(it.first, it.second) }
    }

    override fun onPause() {
        columnsSubscription?.unsubscribe()
        super.onPause()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference === columnsPreference) {
            val fragment = LibraryColumnsDialog.newInstance(preference)
            fragment.setTargetFragment(this, 0)
            fragment.show(childFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun updateColumnsSummary(portraitColumns: Int, landscapeColumns: Int) {
        val portrait = getColumnValue(portraitColumns)
        val landscape = getColumnValue(landscapeColumns)
        val msg = "${getString(R.string.portrait)}: $portrait, ${getString(R.string.landscape)}: $landscape"

        columnsPreference.summary = msg
    }

    private fun getColumnValue(value: Int): String {
        return if (value == 0) getString(R.string.default_columns) else value.toString()
    }

}
