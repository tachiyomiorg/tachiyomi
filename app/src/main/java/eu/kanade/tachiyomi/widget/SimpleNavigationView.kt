package eu.kanade.tachiyomi.widget

import android.annotation.SuppressLint
import android.content.Context
import android.support.design.R
import android.support.design.internal.ScrimInsetsFrameLayout
import android.support.design.widget.TextInputLayout
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.TintTypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.R as TR

@Suppress("LeakingThis")
@SuppressLint("PrivateResource")
open class SimpleNavigationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
: ScrimInsetsFrameLayout(context, attrs, defStyleAttr) {

    /**
     * Max width of the navigation view.
     */
    private var maxWidth: Int

    /**
     * Recycler view containing all the items.
     */
    protected val recycler = RecyclerView(context)

    init {
        // Custom attributes
        val a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.NavigationView, defStyleAttr,
                R.style.Widget_Design_NavigationView)

        ViewCompat.setBackground(
                this, a.getDrawable(R.styleable.NavigationView_android_background))

        if (a.hasValue(R.styleable.NavigationView_elevation)) {
            ViewCompat.setElevation(this, a.getDimensionPixelSize(
                    R.styleable.NavigationView_elevation, 0).toFloat())
        }

        ViewCompat.setFitsSystemWindows(this,
                a.getBoolean(R.styleable.NavigationView_android_fitsSystemWindows, false))

        maxWidth = a.getDimensionPixelSize(R.styleable.NavigationView_android_maxWidth, 0)

        a.recycle()

        recycler.layoutManager = LinearLayoutManager(context)
    }

    /**
     * Overriden to measure the width of the navigation view.
     */
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val width = when (MeasureSpec.getMode(widthSpec)) {
            MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(
                    Math.min(MeasureSpec.getSize(widthSpec), maxWidth), MeasureSpec.EXACTLY)
            MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY)
            else -> widthSpec
        }
        // Let super sort out the height
        super.onMeasure(width, heightSpec)
    }

    /**
     * Base view holder.
     */
    abstract class Holder(view: View) : RecyclerView.ViewHolder(view)

    /**
     * Separator view holder.
     */
    class SeparatorHolder(parent: ViewGroup)
        : Holder(parent.inflate(R.layout.design_navigation_item_separator))

    /**
     * Header view holder.
     */
    class HeaderHolder(parent: ViewGroup)
        : Holder(parent.inflate(R.layout.design_navigation_item_subheader))

    /**
     * Clickable view holder.
     */
    abstract class ClickableHolder(view: View, listener: View.OnClickListener?) : Holder(view) {
        init {
            itemView.setOnClickListener(listener)
        }
    }

    /**
     * Radio view holder.
     */
    class RadioHolder(parent: ViewGroup, listener: View.OnClickListener?)
        : ClickableHolder(parent.inflate(TR.layout.navigation_view_radio), listener) {

        val radio = itemView.findViewById(TR.id.nav_view_item) as RadioButton
    }

    /**
     * Checkbox view holder.
     */
    class CheckboxHolder(parent: ViewGroup, listener: View.OnClickListener?)
        : ClickableHolder(parent.inflate(TR.layout.navigation_view_checkbox), listener) {

        val check = itemView.findViewById(TR.id.nav_view_item) as CheckBox
    }

    /**
     * Multi state view holder.
     */
    class MultiStateHolder(parent: ViewGroup, listener: View.OnClickListener?)
        : ClickableHolder(parent.inflate(TR.layout.navigation_view_checkedtext), listener) {

        val text = itemView.findViewById(TR.id.nav_view_item) as CheckedTextView
    }

    class SpinnerHolder(parent: ViewGroup, listener: OnClickListener? = null)
        : ClickableHolder(parent.inflate(TR.layout.navigation_view_spinner), listener) {

        val text = itemView.findViewById(TR.id.nav_view_item_text) as TextView
        val spinner = itemView.findViewById(TR.id.nav_view_item) as Spinner
    }

    class EditTextHolder(parent: ViewGroup)
        : Holder(parent.inflate(TR.layout.navigation_view_text)) {

        val wrapper  = itemView.findViewById(TR.id.nav_view_item_wrapper) as TextInputLayout
        val edit = itemView.findViewById(TR.id.nav_view_item) as EditText
    }

    protected companion object {
        const val VIEW_TYPE_HEADER = 100
        const val VIEW_TYPE_SEPARATOR = 101
        const val VIEW_TYPE_RADIO = 102
        const val VIEW_TYPE_CHECKBOX = 103
        const val VIEW_TYPE_MULTISTATE = 104
        const val VIEW_TYPE_TEXT = 105
        const val VIEW_TYPE_LIST = 106
    }

}