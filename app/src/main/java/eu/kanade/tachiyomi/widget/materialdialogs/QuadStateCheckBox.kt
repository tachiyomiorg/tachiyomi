package eu.kanade.tachiyomi.widget.materialdialogs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

class QuadStateCheckBox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {

    var state: State = State.UNCHECKED
        set(value) {
            field = value
            updateDrawable()
        }

    private fun updateDrawable() {
        val drawable = when (state) {
            State.UNCHECKED -> tintVector(context, R.drawable.ic_check_box_outline_blank_24dp, R.attr.colorControlNormal)
            State.INDETERMINATE -> tintVector(context, R.drawable.ic_indeterminate_check_box_24dp)
            State.CHECKED -> tintVector(context, R.drawable.ic_check_box_24dp)
            State.INVERSED -> tintVector(context, R.drawable.ic_check_box_x_24dp)
        }

        setImageDrawable(drawable)
    }

    private fun tintVector(context: Context, resId: Int, @AttrRes colorAttrRes: Int = R.attr.colorAccent): Drawable {
        return AppCompatResources.getDrawable(context, resId)!!.apply {
            setTint(context.getResourceColor(colorAttrRes))
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
