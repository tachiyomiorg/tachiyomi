package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.preference_widget_imageview.view.*

class LoginPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Preference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.preference_widget_imageview
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        with(holder.itemView.image_view) {
            if (getPersistedString("").isNullOrEmpty()) {
                setImageResource(android.R.color.transparent)
            } else {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done_green_24dp))
            }
        }
    }

    override public fun notifyChanged() {
        super.notifyChanged()
    }

}
