package eu.kanade.tachiyomi.widget

import android.content.Context
import android.support.annotation.StringRes
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.RelativeLayout
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.dialog_with_checkbox.view.*

class DialogCheckboxView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        LinearLayout(context, attrs) {
    init {
        RelativeLayout.inflate(context, R.layout.dialog_with_checkbox, this)
    }

    fun setDescription(@StringRes id: Int){
        description.text = context.getString(id)
    }

    fun setOptionDescription(@StringRes id: Int){
        checkbox_option.text = context.getString(id)
    }

    fun isChecked(): Boolean {
        return checkbox_option.isChecked
    }
}
