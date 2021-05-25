package eu.kanade.tachiyomi.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.TintTypedArray
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.google.android.material.textfield.TextInputLayout
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.R as TR

@Suppress("LeakingThis")
@SuppressLint("PrivateResource", "RestrictedApi")
open class SimpleNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Recycler view containing all the items.
     */
    protected val recycler = RecyclerView(context)

    init {
        // Custom attributes
        val a = TintTypedArray.obtainStyledAttributes(
            context,
            attrs,
            R.styleable.NavigationView,
            defStyleAttr,
            R.style.Widget_Design_NavigationView
        )

        ViewCompat.setBackground(
            this,
            a.getDrawable(R.styleable.NavigationView_android_background)
        )

        if (a.hasValue(R.styleable.NavigationView_elevation)) {
            ViewCompat.setElevation(
                this,
                a.getDimensionPixelSize(
                    R.styleable.NavigationView_elevation,
                    0
                ).toFloat()
            )
        }

        a.recycle()

        recycler.layoutManager = LinearLayoutManager(context)
    }

    /**
     * Base view holder.
     */
    abstract class Holder(view: View) : RecyclerView.ViewHolder(view)

    /**
     * Separator view holder.
     */
    class SeparatorHolder(parent: ViewGroup) :
        Holder(parent.inflate(R.layout.design_navigation_item_separator))

    /**
     * Header view holder.
     */
    class HeaderHolder(parent: ViewGroup) :
        Holder(parent.inflate(TR.layout.navigation_view_group)) {

        val title: TextView = itemView.findViewById(TR.id.title)
    }

    /**
     * Clickable view holder.
     */
    abstract class ClickableHolder(view: View, listener: OnClickListener?) : Holder(view) {
        init {
            itemView.setOnClickListener(listener)
        }
    }

    /**
     * Radio view holder.
     */
    class RadioHolder(parent: ViewGroup, listener: OnClickListener?) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_radio), listener) {

        val radio: RadioButton = itemView.findViewById(TR.id.nav_view_item)
    }

    /**
     * Checkbox view holder.
     */
    class CheckboxHolder(parent: ViewGroup, listener: OnClickListener?) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_checkbox), listener) {

        val check: CheckBox = itemView.findViewById(TR.id.nav_view_item)
    }

    /**
     * Multi state view holder.
     */
    class MultiStateHolder(parent: ViewGroup, listener: OnClickListener?) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_checkedtext), listener) {

        val text: CheckedTextView = itemView.findViewById(TR.id.nav_view_item)
    }

    class SpinnerHolder(parent: ViewGroup, listener: OnClickListener? = null) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_spinner), listener) {

        val text: TextView = itemView.findViewById(TR.id.nav_view_item_text)
        val spinner: Spinner = itemView.findViewById(TR.id.nav_view_item)
    }

    class EditTextHolder(parent: ViewGroup) :
        Holder(parent.inflate(TR.layout.navigation_view_text)) {

        val wrapper: TextInputLayout = itemView.findViewById(TR.id.nav_view_item_wrapper)
        val edit: EditText = itemView.findViewById(TR.id.nav_view_item)
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
