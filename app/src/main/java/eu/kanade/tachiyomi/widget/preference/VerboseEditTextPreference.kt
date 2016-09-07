package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.support.v7.preference.Preference
import android.util.AttributeSet
import com.afollestad.materialdialogs.MaterialDialog

class VerboseEditTextPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Preference(context, attrs) {

    override fun onAttached() {
        super.onAttached()
        summary = getPrefValue()

        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, any ->
            summary = any as CharSequence?
            true
        }
    }

    fun getPrefValue() = getPersistedString("")

    override fun onClick() {
        super.onClick()

        MaterialDialog.Builder(this.context)
                .title(this.title)
                .input("", getPrefValue(), { dialog, input ->
                    val stringInput = input.toString()
                    persistString(stringInput)
                    summary = stringInput
                })
                .cancelable(true)
                .negativeText(android.R.string.cancel)
                .onNegative { materialDialog, dialogAction -> materialDialog.cancel() }
                .show()
    }
}