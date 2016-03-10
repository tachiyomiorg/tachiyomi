package eu.kanade.tachiyomi.widget.preference

import android.os.Bundle
import android.support.v14.preference.PreferenceDialogFragment
import android.support.v7.preference.Preference
import android.view.View
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import kotlinx.android.synthetic.main.pref_library_columns.view.*

class LibraryColumnsDialog : PreferenceDialogFragment() {

    companion object {

        fun newInstance(preference: Preference): LibraryColumnsDialog {
            val fragment = LibraryColumnsDialog()
            val bundle = Bundle(1)
            bundle.putString("key", preference.key)
            fragment.arguments = bundle
            return fragment
        }
    }

    var portrait: Int = 0
    var landscape: Int = 0

    val preferences: PreferencesHelper
        get() = (activity as SettingsActivity).preferences

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        portrait = preferences.portraitColumns().getOrDefault()
        landscape = preferences.landscapeColumns().getOrDefault()

        view.portrait_columns.value = portrait
        view.landscape_columns.value = landscape

        view.portrait_columns.setOnValueChangedListener { picker, oldValue, newValue ->
            portrait = newValue
        }

        view.landscape_columns.setOnValueChangedListener { picker, oldValue, newValue ->
            landscape = newValue
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            preferences.portraitColumns().set(portrait)
            preferences.landscapeColumns().set(landscape)
        }
    }

}
