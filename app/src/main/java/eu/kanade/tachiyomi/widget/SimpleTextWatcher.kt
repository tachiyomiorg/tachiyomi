package eu.kanade.tachiyomi.widget

import android.text.Editable
import android.text.TextWatcher

open class SimpleTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {}
}
