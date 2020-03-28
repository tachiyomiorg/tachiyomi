package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.pref_badge.view.badge

class BadgePreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    private var badgeNumber: Int = 0

    init {
        widgetLayoutResource = R.layout.pref_badge
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        if (badgeNumber > 0) {
            holder.itemView.badge.text = badgeNumber.toString()
            holder.itemView.badge.visible()
        } else {
            holder.itemView.badge.text = null
            holder.itemView.badge.gone()
        }
    }

    fun setBadge(number: Int) {
        this.badgeNumber = number
        notifyChanged()
    }
}
