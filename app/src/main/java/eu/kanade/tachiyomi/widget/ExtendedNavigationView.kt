package eu.kanade.tachiyomi.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.CallSuper
import android.support.design.R
import android.support.design.internal.ScrimInsetsFrameLayout
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.TintTypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.RadioButton
import android.widget.TextView
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.R as TR

/**
 * An alternative implementation of [android.support.design.widget.NavigationView], without menu
 * inflation and allowing customizable items (multiple selections, custom views, etc).
 */
@Suppress("LeakingThis")
@SuppressLint("PrivateResource")
open class ExtendedNavigationView @JvmOverloads constructor(
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
        addView(recycler)
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
     * Every item of the nav view. Generic items must belong to this list, custom items could be
     * implemented by an abstract class. If more customization is needed in the future, this can be
     * changed to an interface instead of sealed class.
     */
    sealed class Item {
        /**
         * A view separator.
         */
        class Separator(val paddingTop: Int = 0, val paddingBottom: Int = 0) : Item()

        /**
         * A header with a title.
         */
        class Header(val resTitle: Int) : Item()

        /**
         * A checkbox.
         */
        open class Checkbox(val resTitle: Int, var checked: Boolean = false) : Item()

        /**
         * A checkbox belonging to a group. The group must handle selections and restrictions.
         */
        class CheckboxGroup(resTitle: Int, override val group: Group, checked: Boolean = false)
            : Checkbox(resTitle, checked), GroupedItem

        /**
         * A radio belonging to a group (a sole radio makes no sense). The group must handle
         * selections and restrictions.
         */
        class Radio(val resTitle: Int, override val group: Group, var checked: Boolean = false)
            : Item(), GroupedItem

        /**
         * An item with which needs more than two states (selected/deselected).
         */
        abstract class MultiState(val resTitle: Int, var state: Int = 0) : Item() {

            /**
             * Returns the drawable associated to every possible each state.
             */
            abstract fun getStateDrawable(context: Context): Drawable?

            /**
             * Creates a vector tinted with the accent color.
             *
             * @param context any context.
             * @param resId the vector resource to load and tint
             */
            fun tintVector(context: Context, resId: Int): Drawable {
                return VectorDrawableCompat.create(context.resources, resId, context.theme)!!.apply {
                    setTint(context.theme.getResourceColor(TR.attr.colorAccent))
                }
            }
        }

        /**
         * An item with which needs more than two states (selected/deselected) belonging to a group.
         * The group must handle selections and restrictions.
         */
        abstract class MultiStateGroup(resTitle: Int, override val group: Group, state: Int = 0)
            : MultiState(resTitle, state), GroupedItem

        /**
         * A multistate item for sorting lists (unselected, ascending, descending).
         */
        class MultiSort(resId: Int, group: Group) : MultiStateGroup(resId, group) {

            companion object {
                const val SORT_NONE = 0
                const val SORT_ASC = 1
                const val SORT_DESC = 2
            }

            override fun getStateDrawable(context: Context): Drawable? {
                return when (state) {
                    SORT_ASC -> tintVector(context, TR.drawable.ic_keyboard_arrow_up_black_32dp)
                    SORT_DESC -> tintVector(context, TR.drawable.ic_keyboard_arrow_down_black_32dp)
                    SORT_NONE -> ContextCompat.getDrawable(context, TR.drawable.empty_drawable_32dp)
                    else -> null
                }
            }

        }
    }

    /**
     * Interface for an item belonging to a group.
     */
    interface GroupedItem {
        val group: Group
    }

    /**
     * A group containing a list of items.
     */
    interface Group {

        /**
         * An optional header for the group, typically a [Item.Header].
         */
        val header: Item?

        /**
         * An optional footer for the group, typically a [Item.Separator].
         */
        val footer: Item?

        /**
         * The items of the group, excluding header and footer.
         */
        val items: List<Item>

        /**
         * Creates all the elements of this group. Implementations can override this method for more
         * customization.
         */
        fun createItems() = (mutableListOf<Item>() + header + items + footer).filterNotNull()

        /**
         * Called after creating the list of items. Implementations should load the current values
         * into the models.
         */
        fun initModels()

        /**
         * Called when an item of this group is clicked. The group is responsible for all the
         * selections of its items.
         */
        fun onItemClicked(item: Item)

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

    /**
     * Base adapter for the navigation view. It knows how to create and render every subclass of
     * [Item].
     */
    abstract inner class Adapter(private val items: List<Item>) : RecyclerView.Adapter<Holder>() {

        private val onClick = View.OnClickListener {
            val pos = recycler.getChildAdapterPosition(it)
            val item = items[pos]
            onItemClicked(item)
        }

        fun notifyItemChanged(item: Item) {
            val pos = items.indexOf(item)
            if (pos != -1) notifyItemChanged(pos)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        @CallSuper
        override fun getItemViewType(position: Int): Int {
            val item = items[position]
            return when (item) {
                is Item.Header -> VIEW_TYPE_HEADER
                is Item.Separator -> VIEW_TYPE_SEPARATOR
                is Item.Radio -> VIEW_TYPE_RADIO
                is Item.Checkbox -> VIEW_TYPE_CHECKBOX
                is Item.MultiState -> VIEW_TYPE_MULTISTATE
            }
        }

        @CallSuper
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> HeaderHolder(parent)
                VIEW_TYPE_SEPARATOR -> SeparatorHolder(parent)
                VIEW_TYPE_RADIO -> RadioHolder(parent, onClick)
                VIEW_TYPE_CHECKBOX -> CheckboxHolder(parent, onClick)
                VIEW_TYPE_MULTISTATE -> MultiStateHolder(parent, onClick)
                else -> throw Exception("Unknown view type")
            }
        }

        @CallSuper
        override fun onBindViewHolder(holder: Holder, position: Int) {
            when (holder) {
                is HeaderHolder -> {
                    val view = holder.itemView as TextView
                    val item = items[position] as Item.Header
                    view.setText(item.resTitle)
                }
                is SeparatorHolder -> {
                    val view = holder.itemView
                    val item = items[position] as Item.Separator
                    view.setPadding(0, item.paddingTop, 0, item.paddingBottom)
                }
                is RadioHolder -> {
                    val item = items[position] as Item.Radio
                    holder.radio.setText(item.resTitle)
                    holder.radio.isChecked = item.checked
                }
                is CheckboxHolder -> {
                    val item = items[position] as Item.CheckboxGroup
                    holder.check.setText(item.resTitle)
                    holder.check.isChecked = item.checked
                }
                is MultiStateHolder -> {
                    val item = items[position] as Item.MultiStateGroup
                    val drawable = item.getStateDrawable(context)
                    holder.text.setText(item.resTitle)
                    holder.text.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                }
            }
        }

        abstract fun onItemClicked(item: Item)

    }

    companion object {
        private const val VIEW_TYPE_HEADER = 100
        private const val VIEW_TYPE_SEPARATOR = 101
        private const val VIEW_TYPE_RADIO = 102
        private const val VIEW_TYPE_CHECKBOX = 103
        private const val VIEW_TYPE_MULTISTATE = 104
    }

}