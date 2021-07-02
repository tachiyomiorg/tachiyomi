package eu.kanade.tachiyomi.widget.materialdialogs

import androidx.annotation.CheckResult
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter

/**
 * A variant of listItemsMultiChoice that allows for checkboxes that supports 4 states instead.
 */
@CheckResult
fun MaterialDialog.listItemsQuadStateMultiChoice(
    items: List<CharSequence>,
    disabledIndices: IntArray? = null,
    initialSelected: IntArray = IntArray(items.size),
    selection: QuadStateMultiChoiceListener
): MaterialDialog {
    return customListAdapter(
        QuadStateMultiChoiceDialogAdapter(
            dialog = this,
            items = items,
            disabledItems = disabledIndices,
            initialSelected = initialSelected,
            selection = selection
        )
    )
}
