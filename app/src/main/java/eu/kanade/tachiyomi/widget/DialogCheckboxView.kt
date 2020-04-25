package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.inflate
import kotlinx.android.synthetic.main.common_dialog_with_checkbox.view.checkbox_option
import kotlinx.android.synthetic.main.common_dialog_with_checkbox.view.description

class DialogCheckboxView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    init {
        addView(inflate(R.layout.common_dialog_with_checkbox))
    }

    fun setDescription(@StringRes id: Int) {
        description.text = context.getString(id)
    }

    fun setOptionDescription(@StringRes id: Int) {
        checkbox_option.text = context.getString(id)
    }

    fun isChecked(): Boolean {
        return checkbox_option.isChecked
    }
}
