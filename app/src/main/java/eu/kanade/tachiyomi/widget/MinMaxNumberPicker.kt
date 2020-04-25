package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.NumberPicker
import eu.kanade.tachiyomi.R

class MinMaxNumberPicker @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NumberPicker(context, attrs) {

    init {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.MinMaxNumberPicker, 0, 0)
            try {
                minValue = ta.getInt(R.styleable.MinMaxNumberPicker_min, 0)
                maxValue = ta.getInt(R.styleable.MinMaxNumberPicker_max, 0)
            } finally {
                ta.recycle()
            }
        }
    }
}
