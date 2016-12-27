package eu.kanade.tachiyomi.ui.catalogue

import android.content.Context
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.CheckBox
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.online.OnlineSource.Filter
import eu.kanade.tachiyomi.data.source.online.OnlineSource.FilterState

class FilterAdapter(val filterStates: List<FilterState>) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {
    val states = IntArray(filterStates.size);
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterAdapter.ViewHolder {
        val vh = ThreeStateCheckBox(parent.context)
        return ViewHolder(vh)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filterState = filterStates[position];
        states[position] = filterState.state
        holder.checkBox.setText(filterState.filter.name)
        holder.checkBox.threeState = filterState.filter.type == Filter.TYPE_IGNORE_INCLUDE_EXCLUDE
        holder.checkBox.position = position
        holder.checkBox.updateButton()
    }

    override fun getItemCount(): Int {
        return filterStates.size
    }

    class ViewHolder(val checkBox: ThreeStateCheckBox) : RecyclerView.ViewHolder(checkBox)

    inner class ThreeStateCheckBox(context: Context?, var position: Int = 0, var threeState: Boolean = false) : CheckBox(context) {
        init {
            setOnCheckedChangeListener { buttonView, isChecked ->
                states[position] = if (threeState) (states[position] + 1) % 3 else
                    if (isChecked) Filter.STATE_INCLUDE else Filter.STATE_IGNORE
                updateButton()
            }
            updateButton()
        }

        fun updateButton() {
            setButtonDrawable(VectorDrawableCompat.create(getResources(),
                    arrayOf(R.drawable.ic_check_box_outline_blank_24dp, R.drawable.ic_check_box_24dp, R.drawable.ic_check_box_x_24dp)[states[position]],
                    null))
            invalidate()
        }
    }
}