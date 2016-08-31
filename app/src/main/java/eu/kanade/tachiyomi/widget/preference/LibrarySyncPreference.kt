package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.librarysync.LibrarySyncManager
import kotlinx.android.synthetic.main.preference_widget_imageview_switch.view.*
import uy.kohesive.injekt.injectLazy

class LibrarySyncPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Preference(context, attrs) {

    val sync: LibrarySyncManager by injectLazy()

    init {
        widgetLayoutResource = R.layout.preference_widget_imageview_switch
    }

    var imageResource: Int? = null

    override fun onAttached() {
        super.onAttached()
        updatePreferenceUI()
    }

    /**
     * Select correct status icon and set correct summary
     */
    fun updatePreferenceUI() {
        imageResource = if (getPrefValue()) {
            //Sync enabled
            if (sync.syncAvailable) {
                //Sync is working
                if (sync.lastFailedSyncCount > 5) {
                    //But last 5+ syncs have failed!
                    setSummary(R.string.sync_status_error_many_failed)
                    R.drawable.ic_warning_yellow_24dp
                } else {
                    //Sync is OK
                    setSummary(R.string.sync_status_ok)
                    R.drawable.ic_done_green_24dp
                }
            } else {
                //Something is wrong with sync
                setSummary(sync.syncError)
                R.drawable.ic_warning_yellow_24dp
            }
        } else {
            //Sync not enabled
            setSummary(R.string.sync_status_not_enabled)
            android.R.color.transparent
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        //Set selected status icon
        holder.itemView.switch_image_view.setImageResource(imageResource!!)

        //Set switch value
        holder.itemView.switch_switch_view.setOnCheckedChangeListener(null)
        holder.itemView.switch_switch_view.isChecked = getPrefValue()
        holder.itemView.switch_switch_view.setOnCheckedChangeListener { compoundButton, b ->
            persistBoolean(b)
            notifyChanged()
        }
    }

    private fun getPrefValue() = getPersistedBoolean(false)

    override fun onClick() {
        super.onClick()
        persistBoolean(!getPrefValue())
        notifyChanged()
    }

    public override fun notifyChanged() {
        super.notifyChanged()
        updatePreferenceUI()
    }
}
