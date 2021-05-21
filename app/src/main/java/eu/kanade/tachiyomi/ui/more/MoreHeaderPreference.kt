package eu.kanade.tachiyomi.ui.more

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import eu.kanade.tachiyomi.R

class MoreHeaderPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    init {
        layoutResource = R.layout.pref_more_header
        isSelectable = false
    }
}
