package eu.kanade.tachiyomi.util.preference

import android.widget.CompoundButton
import com.tfcporciuncula.flow.Preference

/**
 * Binds a checkbox or switch view with a boolean preference.
 */
fun CompoundButton.bindToPreference(pref: Preference<Boolean>) {
    isChecked = pref.get()
    setOnCheckedChangeListener { _, isChecked -> pref.set(isChecked) }
}
