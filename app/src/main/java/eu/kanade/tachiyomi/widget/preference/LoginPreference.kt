package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import android.util.AttributeSet
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.pref_widget_imageview.view.*

class LoginPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Preference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.pref_widget_imageview
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.image_view.setImageResource(if (getPersistedString("").isNullOrEmpty())
            android.R.color.transparent
        else
            R.drawable.ic_done_green_24dp)
    }

    override public fun notifyChanged() {
        super.notifyChanged()
    }

}
