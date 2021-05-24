package eu.kanade.tachiyomi.ui.more

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.TooltipCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.view.setTooltip

class AboutLinksPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    init {
        layoutResource = R.layout.pref_about_links
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(R.id.btn_website).apply {
            setTooltip(R.string.website)
            setOnClickListener { context.openInBrowser("https://tachiyomi.org") }
        }
        holder.findViewById(R.id.btn_discord).apply {
            TooltipCompat.setTooltipText(this, "Discord")
            setOnClickListener { context.openInBrowser("https://discord.gg/tachiyomi") }
        }
        holder.findViewById(R.id.btn_twitter).apply {
            TooltipCompat.setTooltipText(this, "Twitter")
            setOnClickListener { context.openInBrowser("https://twitter.com/tachiyomiorg") }
        }
        holder.findViewById(R.id.btn_facebook).apply {
            TooltipCompat.setTooltipText(this, "Facebook")
            setOnClickListener { context.openInBrowser("https://facebook.com/tachiyomiorg") }
        }
        holder.findViewById(R.id.btn_github).apply {
            TooltipCompat.setTooltipText(this, "GitHub")
            setOnClickListener { context.openInBrowser("https://github.com/tachiyomiorg") }
        }
    }
}
