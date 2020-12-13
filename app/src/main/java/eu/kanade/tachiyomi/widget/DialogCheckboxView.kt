package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.databinding.CommonDialogWithCheckboxBinding

class DialogCheckboxView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val binding: CommonDialogWithCheckboxBinding

    init {
        binding = CommonDialogWithCheckboxBinding.inflate(LayoutInflater.from(context), this, false)
        addView(binding.root)
    }

    fun setDescription(@StringRes id: Int) {
        binding.description.text = context.getString(id)
    }

    fun setOptionDescription(@StringRes id: Int) {
        binding.checkboxOption.text = context.getString(id)
    }

    fun isChecked(): Boolean {
        return binding.checkboxOption.isChecked
    }
}
