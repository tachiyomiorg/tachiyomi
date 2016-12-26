package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.LocaleHelper
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.widget.preference.IntListPreference
import eu.kanade.tachiyomi.widget.preference.LibraryColumnsDialog
import eu.kanade.tachiyomi.widget.preference.SimpleDialogPreference
import net.xpece.android.support.preference.ListPreference
import net.xpece.android.support.preference.MultiSelectListPreference
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.injectLazy

class SettingsGeneralFragment : SettingsFragment(),
        PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {


    companion object {
        fun newInstance(rootKey: String): SettingsGeneralFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsGeneralFragment().apply { arguments = args }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    private val db: DatabaseHelper by injectLazy()

    val columnsPreference: SimpleDialogPreference by bindPref(R.string.pref_library_columns_dialog_key)

    val updateInterval: IntListPreference by bindPref(R.string.pref_library_update_interval_key)

    val updateRestriction: MultiSelectListPreference by bindPref(R.string.pref_library_update_restriction_key)

    val themePreference: IntListPreference by bindPref(R.string.pref_theme_key)

    val categoryUpdate: MultiSelectListPreference by bindPref(R.string.pref_library_update_categories_key)

    val langPreference: ListPreference by bindPref(R.string.pref_language_key)

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        subscriptions += preferences.libraryUpdateInterval().asObservable()
                .subscribe { updateRestriction.isVisible = it > 0 }

        subscriptions += Observable.combineLatest(
                preferences.portraitColumns().asObservable(),
                preferences.landscapeColumns().asObservable())
                { portraitColumns, landscapeColumns -> Pair(portraitColumns, landscapeColumns) }
                .subscribe { updateColumnsSummary(it.first, it.second) }

        updateInterval.setOnPreferenceChangeListener { preference, newValue ->
            // Always cancel the previous task, it seems that sometimes they are not updated.
            LibraryUpdateJob.cancelTask()

            val interval = (newValue as String).toInt()
            if (interval > 0) {
                LibraryUpdateJob.setupTask(interval)
            }
            true
        }

        updateRestriction.setOnPreferenceChangeListener { preference, newValue ->
            // Post to event looper to allow the preference to be updated.
            subscriptions += Observable.fromCallable {
                LibraryUpdateJob.setupTask()
            }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()

            true
        }

        val dbCategories = db.getCategories().executeAsBlocking()
        categoryUpdate.apply {
            entries = dbCategories.map { it.name }.toTypedArray()
            entryValues = dbCategories.map { it.id.toString() }.toTypedArray()
        }

        subscriptions += preferences.libraryUpdateCategories().asObservable()
                .subscribe {
                    val selectedCategories = it
                            .mapNotNull { id -> dbCategories.find { it.id == id.toInt() } }
                            .sortedBy { it.order }

                    val summary = if (selectedCategories.isEmpty())
                        getString(R.string.all)
                    else
                        selectedCategories.joinToString { it.name }

                    categoryUpdate.summary = summary
                }

        themePreference.setOnPreferenceChangeListener { preference, newValue ->
            (activity as SettingsActivity).parentFlags = SettingsActivity.FLAG_THEME_CHANGED
            activity.recreate()
            true
        }

        val langValues = langPreference.entryValues.map { value ->
            val locale = LocaleHelper.getLocaleFromString(value.toString())
            locale?.getDisplayName(locale)?.capitalize() ?: context.getString(R.string.system_default)
        }

        langPreference.entries = langValues.toTypedArray()
        langPreference.setOnPreferenceChangeListener { preference, newValue ->
            (activity as SettingsActivity).parentFlags = SettingsActivity.FLAG_LANG_CHANGED
            LocaleHelper.changeLocale(newValue.toString())
            val app = activity.application
            LocaleHelper.updateConfiguration(app, app.resources.configuration)
            activity.recreate()
            true
        }

    }

    override fun onPreferenceDisplayDialog(p0: PreferenceFragmentCompat?, p: Preference): Boolean {
        if (p === columnsPreference) {
            val fragment = LibraryColumnsDialog.newInstance(p)
            fragment.setTargetFragment(this, 0)
            fragment.show(fragmentManager, null)
            return true
        }
        return false
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
