package eu.kanade.tachiyomi.widget.materialdialogs

import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.databinding.DialogStubQuadstatemultichoiceBinding
import eu.kanade.tachiyomi.databinding.DialogStubTextinputBinding

fun MaterialAlertDialogBuilder.setTextInput(
    hint: String? = null,
    prefill: String? = null,
    onTextChanged: (String) -> Unit
): MaterialAlertDialogBuilder {
    val binding = DialogStubTextinputBinding.inflate(LayoutInflater.from(context))
    binding.textField.hint = hint
    binding.textField.editText?.apply {
        setText(prefill, TextView.BufferType.EDITABLE)
        doAfterTextChanged {
            onTextChanged(it?.toString() ?: "")
        }
        post {
            requestFocusFromTouch()
            context.getSystemService<InputMethodManager>()?.showSoftInput(this, 0)
        }
    }
    return setView(binding.root)
}

/**
 * Sets a list of items with checkboxes that supports 4 states.
 *
 * @see eu.kanade.tachiyomi.widget.materialdialogs.QuadStateTextView
 */
fun MaterialAlertDialogBuilder.setQuadStateMultiChoiceItems(
    items: List<CharSequence>,
    initialSelected: IntArray,
    disabledIndices: IntArray? = null,
    selection: QuadStateMultiChoiceListener
): MaterialAlertDialogBuilder {
    val binding = DialogStubQuadstatemultichoiceBinding.inflate(LayoutInflater.from(context))
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = QuadStateMultiChoiceDialogAdapter(
        items = items,
        disabledItems = disabledIndices,
        initialSelected = initialSelected,
        listener = selection
    )
    setView(binding.root)
    return this
}
