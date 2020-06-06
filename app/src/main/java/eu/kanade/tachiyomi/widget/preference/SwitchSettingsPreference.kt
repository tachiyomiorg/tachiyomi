package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.pref_settings.view.button

class SwitchSettingsPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SwitchPreferenceCompat(context, attrs) {

    var onSettingsClick: View.OnClickListener? = null

    init {
        widgetLayoutResource = R.layout.pref_settings
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.button.setOnClickListener {
            onSettingsClick?.onClick(it)
        }
    }
}
