package eu.kanade.tachiyomi.widget.materialdialogs

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.databinding.DialogQuadstatemultichoiceItemBinding

internal class QuadStateMultiChoiceViewHolder(
    itemBinding: DialogQuadstatemultichoiceItemBinding,
    private val adapter: QuadStateMultiChoiceDialogAdapter
) : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
    init {
        itemView.setOnClickListener(this)
    }

    val controlView = itemBinding.quadStateControl

    var isEnabled: Boolean
        get() = itemView.isEnabled
        set(value) {
            itemView.isEnabled = value
            controlView.isEnabled = value
        }

    override fun onClick(view: View) = adapter.itemClicked(bindingAdapterPosition)
}
