package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.graphics.Color
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.setVectorCompat
import kotlinx.android.synthetic.main.pref_item_source.view.*
import net.xpece.android.support.preference.CheckBoxPreference

class LoginCheckBoxPreference @JvmOverloads constructor(
        context: Context,
        val source: HttpSource,
        attrs: AttributeSet? = null
) : CheckBoxPreference(context, attrs) {

    init {
        layoutResource = R.layout.pref_item_source
    }

    private var onLoginClick: () -> Unit = {}

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val loginFrame = holder.itemView.login_frame
        if (source is LoginSource) {
            val tint = if (source.isLogged())
                Color.argb(255, 76, 175, 80)
            else
                context.getResourceColor(android.R.attr.textColorSecondary)

            holder.itemView.login.setVectorCompat(R.drawable.ic_account_circle_black_24dp, tint)

            loginFrame.visibility = View.VISIBLE
            loginFrame.setOnClickListener {
                onLoginClick()
            }
        } else {
            loginFrame.visibility = View.GONE
        }
    }

    fun setOnLoginClickListener(block: () -> Unit) {
        onLoginClick = block
    }

    // Make method public
    override public fun notifyChanged() {
        super.notifyChanged()
    }

}