package eu.kanade.tachiyomi.widget.preference

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.R

class SwitchSettingsPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SwitchPreferenceCompat(context, attrs) {

    var onSettingsClick: View.OnClickListener? = null

    init {
        widgetLayoutResource = R.layout.pref_settings
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(R.id.button).setOnClickListener {
            onSettingsClick?.onClick(it)
        }

        // Disable swiping to align with SwitchPreferenceCompat
        holder.findViewById(R.id.switchWidget).setOnTouchListener { _, event ->
            event.actionMasked == MotionEvent.ACTION_MOVE
        }
    }
}
