package eu.kanade.tachiyomi.widget.materialdialogs

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.setVectorCompat

class QuadStateCheckBox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {

    var state: State = State.UNCHECKED
        set(value) {
            field = value
            updateDrawable()
        }

    private fun updateDrawable() {
        when (state) {
            State.UNCHECKED -> setVectorCompat(R.drawable.ic_check_box_outline_blank_24dp, R.attr.colorControlNormal)
            State.INDETERMINATE -> setVectorCompat(R.drawable.ic_indeterminate_check_box_24dp, R.attr.colorAccent)
            State.CHECKED -> setVectorCompat(R.drawable.ic_check_box_24dp, R.attr.colorAccent)
            State.INVERSED -> setVectorCompat(R.drawable.ic_check_box_x_24dp, R.attr.colorAccent)
        }
    }

    enum class State {
        UNCHECKED,
        INDETERMINATE,
        CHECKED,
        INVERSED,
        ;
    }
}
