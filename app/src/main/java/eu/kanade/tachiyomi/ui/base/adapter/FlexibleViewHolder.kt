package eu.kanade.tachiyomi.ui.base.adapter

import android.support.v7.widget.RecyclerView
import android.view.View

import eu.davidea.flexibleadapter4.FlexibleAdapter

abstract class FlexibleViewHolder(view: View,
                                  private val adapter: FlexibleAdapter<*, *>,
                                  private val itemClickListener: FlexibleViewHolder.OnListItemClickListener) :
        RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

    init {
        view.setOnClickListener(this)
        view.setOnLongClickListener(this)
    }

    override fun onClick(view: View) {
        if (itemClickListener.onListItemClick(adapterPosition)) {
            toggleActivation()
        }
    }

    override fun onLongClick(view: View): Boolean {
        itemClickListener.onListItemLongClick(adapterPosition)
        toggleActivation()
        return true
    }

    fun toggleActivation() {
        itemView.isActivated = adapter.isSelected(adapterPosition)
    }

    interface OnListItemClickListener {
        fun onListItemClick(position: Int): Boolean
        fun onListItemLongClick(position: Int)
    }

}