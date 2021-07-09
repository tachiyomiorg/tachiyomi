package eu.kanade.tachiyomi.widget.materialdialogs

import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
