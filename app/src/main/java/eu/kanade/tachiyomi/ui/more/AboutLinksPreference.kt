package eu.kanade.tachiyomi.ui.more

import android.content.Context
import android.util.AttributeSet
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
            setTooltip(contentDescription.toString())
            setOnClickListener { context.openInBrowser("https://tachiyomi.org") }
        }
        holder.findViewById(R.id.btn_discord).apply {
            setTooltip(contentDescription.toString())
            setOnClickListener { context.openInBrowser("https://discord.gg/tachiyomi") }
        }
        holder.findViewById(R.id.btn_twitter).apply {
            setTooltip(contentDescription.toString())
            setOnClickListener { context.openInBrowser("https://twitter.com/tachiyomiorg") }
        }
        holder.findViewById(R.id.btn_facebook).apply {
            setTooltip(contentDescription.toString())
            setOnClickListener { context.openInBrowser("https://facebook.com/tachiyomiorg") }
        }
        holder.findViewById(R.id.btn_reddit).apply {
            setTooltip(contentDescription.toString())
            setOnClickListener { context.openInBrowser("https://www.reddit.com/r/Tachiyomi") }
        }
        holder.findViewById(R.id.btn_github).apply {
            setTooltip(contentDescription.toString())
            setOnClickListener { context.openInBrowser("https://github.com/tachiyomiorg") }
        }
    }
}
