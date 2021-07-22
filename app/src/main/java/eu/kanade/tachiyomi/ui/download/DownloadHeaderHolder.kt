package eu.kanade.tachiyomi.ui.download

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.ExpandableViewHolder
import eu.kanade.tachiyomi.databinding.DownloadHeaderBinding

class DownloadHeaderHolder(view: View, adapter: FlexibleAdapter<*>) : ExpandableViewHolder(view, adapter) {

    private val binding = DownloadHeaderBinding.bind(view)

    @SuppressLint("SetTextI18n")
    fun bind(item: DownloadHeaderItem) {
        setDragHandleView(binding.reorder)
        binding.title.text = "${item.name} (${item.size})"
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            binding.container.isDragged = true
            mAdapter.collapseAll()
        }
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        binding.container.isDragged = false
        mAdapter as DownloadAdapter
        mAdapter.expandAll()
        mAdapter.downloadItemListener.onItemReleased(position)
    }
}
