package eu.kanade.tachiyomi.ui.catalogue

import android.content.Context
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.online.OnlineSource.Filter
import eu.kanade.tachiyomi.util.dpToPx
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import eu.kanade.tachiyomi.widget.SimpleNavigationView
import eu.kanade.tachiyomi.widget.SimpleTextWatcher
import kotlinx.android.synthetic.main.catalogue_drawer_content.view.*


class CatalogueNavigationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : SimpleNavigationView(context, attrs) {

    val adapter = Adapter()

    var onSearchClicked = {}

    var onResetClicked = {}

    init {
        recycler.adapter = adapter
        val view = inflate(R.layout.catalogue_drawer_content)
        (view as ViewGroup).addView(recycler)
        addView(view)

        search_btn.setOnClickListener { onSearchClicked() }
        reset_btn.setOnClickListener { onResetClicked() }
    }

    fun setFilters(items: List<Filter<*>>) {
        adapter.items = items
        adapter.notifyDataSetChanged()
    }

    inner class Adapter : RecyclerView.Adapter<Holder>() {

        var items: List<Filter<*>> = emptyList()

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is Filter.Header -> VIEW_TYPE_HEADER
                is Filter.CheckBox -> VIEW_TYPE_CHECKBOX
                is Filter.TriState -> VIEW_TYPE_MULTISTATE
                is Filter.List<*> -> VIEW_TYPE_LIST
                is Filter.Text -> VIEW_TYPE_TEXT
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> HeaderHolder(parent)
                VIEW_TYPE_CHECKBOX -> CheckboxHolder(parent, null)
                VIEW_TYPE_MULTISTATE -> MultiStateHolder(parent, null).apply {
                    // Adjust view with checkbox
                    text.setPadding(4.dpToPx, 0, 0, 0)
                    text.compoundDrawablePadding = 20.dpToPx
                }
                VIEW_TYPE_LIST -> SpinnerHolder(parent)
                VIEW_TYPE_TEXT -> EditTextHolder(parent)
                else -> throw Exception("Unknown view type")
            }
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val filter = items[position]
            when (filter) {
                is Filter.Header -> {
                    val view = holder.itemView as TextView
                    view.visibility = if (filter.name.isEmpty()) View.GONE else View.VISIBLE
                    view.text = filter.name
                }
                is Filter.CheckBox -> {
                    val view = (holder as CheckboxHolder).check
                    view.text = filter.name
                    view.isChecked = filter.state
                    holder.itemView.setOnClickListener {
                        view.toggle()
                        filter.state = view.isChecked
                    }
                }
                is Filter.TriState -> {
                    val view = (holder as MultiStateHolder).text
                    view.text = filter.name

                    fun getIcon() = VectorDrawableCompat.create(view.resources, when (filter.state) {
                        Filter.TriState.STATE_IGNORE -> R.drawable.ic_check_box_outline_blank_24dp
                        Filter.TriState.STATE_INCLUDE -> R.drawable.ic_check_box_24dp
                        Filter.TriState.STATE_EXCLUDE -> R.drawable.ic_check_box_x_24dp
                        else -> throw Exception("Unknown state")
                    }, null)?.apply {
                        val color = if (filter.state == Filter.TriState.STATE_INCLUDE)
                            R.attr.colorAccent
                        else
                            android.R.attr.textColorSecondary

                        setTint(view.context.theme.getResourceColor(color))
                    }

                    view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
                    holder.itemView.setOnClickListener {
                        filter.state = (filter.state + 1) % 3
                        view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
                    }
                }
                is Filter.List<*> -> {
                    holder as SpinnerHolder
                    holder.text.text = filter.name + ": "

                    val spinner = holder.spinner
                    spinner.prompt = filter.name
                    spinner.adapter = ArrayAdapter<Any>(holder.itemView.context,
                            android.R.layout.simple_spinner_item, filter.values).apply {
                        setDropDownViewResource(R.layout.spinner_item)
                    }
                    spinner.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
                        filter.state = position
                    }
                    spinner.setSelection(filter.state)
                }
                is Filter.Text -> {
                    holder as EditTextHolder
                    holder.wrapper.visibility = if (filter.name.isEmpty()) View.GONE else View.VISIBLE
                    holder.wrapper.hint = filter.name
                    holder.edit.setText(filter.state)
                    holder.edit.addTextChangedListener(object : SimpleTextWatcher() {
                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                            filter.state = s.toString()
                        }
                    })
                }
            }
        }

    }

}