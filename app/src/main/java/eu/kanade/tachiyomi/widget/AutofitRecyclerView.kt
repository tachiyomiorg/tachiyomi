package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

class AutofitRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    RecyclerView(context, attrs) {

    private val manager = GridLayoutManager(context, 1)

    private var columnWidth = -1

    var spanCount = 0
        set(value) {
            field = value
            if (value > 0) {
                manager.spanCount = value
            }
        }

    val itemWidth: Int
        get() = measuredWidth / manager.spanCount

    init {
        if (attrs != null) {
            val attrsArray = intArrayOf(android.R.attr.columnWidth)
            val array = context.obtainStyledAttributes(attrs, attrsArray)
            columnWidth = array.getDimensionPixelSize(0, -1)
            array.recycle()
        }

        layoutManager = manager
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        if (spanCount == 0 && columnWidth > 0) {
            val count = max(1, measuredWidth / columnWidth)
            spanCount = count
        }
    }
}
