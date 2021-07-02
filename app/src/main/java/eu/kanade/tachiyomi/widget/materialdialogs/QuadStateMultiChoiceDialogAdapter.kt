package eu.kanade.tachiyomi.widget.materialdialogs

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.list.DialogAdapter
import com.afollestad.materialdialogs.list.getItemSelector
import com.afollestad.materialdialogs.utils.MDUtil.inflate
import com.afollestad.materialdialogs.utils.MDUtil.maybeSetTextColor
import eu.kanade.tachiyomi.R

private object CheckPayload
private object InverseCheckPayload
private object UncheckPayload

typealias QuadStateMultiChoiceListener = (indices: IntArray) -> Unit

internal class QuadStateMultiChoiceDialogAdapter(
    private var dialog: MaterialDialog,
    internal var items: List<CharSequence>,
    disabledItems: IntArray?,
    initialSelected: IntArray,
    internal var selection: QuadStateMultiChoiceListener
) : RecyclerView.Adapter<QuadStateMultiChoiceViewHolder>(),
    DialogAdapter<CharSequence, QuadStateMultiChoiceListener> {

    private val states = QuadStateCheckBox.State.values()

    private var currentSelection: IntArray = initialSelected
        set(value) {
            val previousSelection = field
            field = value
            previousSelection.forEachIndexed { index, previous ->
                val current = value[index]
                when {
                    current == QuadStateCheckBox.State.CHECKED.ordinal && previous != QuadStateCheckBox.State.CHECKED.ordinal -> {
                        // This value was selected
                        notifyItemChanged(index, CheckPayload)
                    }
                    current == QuadStateCheckBox.State.INVERSED.ordinal && previous != QuadStateCheckBox.State.INVERSED.ordinal -> {
                        // This value was inverse selected
                        notifyItemChanged(index, InverseCheckPayload)
                    }
                    current == QuadStateCheckBox.State.UNCHECKED.ordinal && previous != QuadStateCheckBox.State.UNCHECKED.ordinal -> {
                        // This value was unselected
                        notifyItemChanged(index, UncheckPayload)
                    }
                }
            }
        }
    private var disabledIndices: IntArray = disabledItems ?: IntArray(0)

    internal fun itemClicked(index: Int) {
        val newSelection = this.currentSelection.toMutableList()
        newSelection[index] = when (currentSelection[index]) {
            QuadStateCheckBox.State.CHECKED.ordinal -> QuadStateCheckBox.State.INVERSED.ordinal
            QuadStateCheckBox.State.INVERSED.ordinal -> QuadStateCheckBox.State.UNCHECKED.ordinal
            // INDETERMINATE or UNCHECKED
            else -> QuadStateCheckBox.State.CHECKED.ordinal
        }
        this.currentSelection = newSelection.toIntArray()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuadStateMultiChoiceViewHolder {
        val listItemView: View = parent.inflate(dialog.windowContext, R.layout.md_listitem_quadstatemultichoice)
        val viewHolder = QuadStateMultiChoiceViewHolder(
            itemView = listItemView,
            adapter = this
        )
        viewHolder.titleView.maybeSetTextColor(dialog.windowContext, R.attr.md_color_content)

        return viewHolder
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(
        holder: QuadStateMultiChoiceViewHolder,
        position: Int
    ) {
        holder.isEnabled = !disabledIndices.contains(position)

        holder.controlView.state = states[currentSelection[position]]
        holder.titleView.text = items[position]
        holder.itemView.background = dialog.getItemSelector()

        if (dialog.bodyFont != null) {
            holder.titleView.typeface = dialog.bodyFont
        }
    }

    override fun onBindViewHolder(
        holder: QuadStateMultiChoiceViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        when (payloads.firstOrNull()) {
            CheckPayload -> {
                holder.controlView.state = QuadStateCheckBox.State.CHECKED
                return
            }
            InverseCheckPayload -> {
                holder.controlView.state = QuadStateCheckBox.State.INVERSED
                return
            }
            UncheckPayload -> {
                holder.controlView.state = QuadStateCheckBox.State.UNCHECKED
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun positiveButtonClicked() {
        selection.invoke(currentSelection)
    }

    override fun replaceItems(
        items: List<CharSequence>,
        listener: QuadStateMultiChoiceListener?
    ) {
        this.items = items
        if (listener != null) {
            this.selection = listener
        }
        this.notifyDataSetChanged()
    }

    override fun disableItems(indices: IntArray) {
        this.disabledIndices = indices
        notifyDataSetChanged()
    }

    override fun checkItems(indices: IntArray) {
        val newSelection = this.currentSelection.toMutableList()
        for (index in indices) {
            newSelection[index] = QuadStateCheckBox.State.CHECKED.ordinal
        }
        this.currentSelection = newSelection.toIntArray()
    }

    override fun uncheckItems(indices: IntArray) {
        val newSelection = this.currentSelection.toMutableList()
        for (index in indices) {
            newSelection[index] = QuadStateCheckBox.State.UNCHECKED.ordinal
        }
        this.currentSelection = newSelection.toIntArray()
    }

    override fun toggleItems(indices: IntArray) {
        val newSelection = this.currentSelection.toMutableList()
        for (index in indices) {
            if (this.disabledIndices.contains(index)) {
                continue
            }

            if (this.currentSelection[index] != QuadStateCheckBox.State.CHECKED.ordinal) {
                newSelection[index] = QuadStateCheckBox.State.CHECKED.ordinal
            } else {
                newSelection[index] = QuadStateCheckBox.State.UNCHECKED.ordinal
            }
        }
        this.currentSelection = newSelection.toIntArray()
    }

    override fun checkAllItems() {
        this.currentSelection = IntArray(itemCount) { QuadStateCheckBox.State.CHECKED.ordinal }
    }

    override fun uncheckAllItems() {
        this.currentSelection = IntArray(itemCount) { QuadStateCheckBox.State.UNCHECKED.ordinal }
    }

    override fun toggleAllChecked() {
        if (this.currentSelection.any { it != QuadStateCheckBox.State.CHECKED.ordinal }) {
            checkAllItems()
        } else {
            uncheckAllItems()
        }
    }

    override fun isItemChecked(index: Int) = this.currentSelection[index] == QuadStateCheckBox.State.CHECKED.ordinal
}
