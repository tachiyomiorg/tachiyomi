package eu.kanade.tachiyomi.ui.catalogue

import android.content.Context
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.online.OnlineSource.Filter
import eu.kanade.tachiyomi.data.source.online.OnlineSource.FilterState
import android.text.TextWatcher
import android.text.Editable
import android.view.inputmethod.EditorInfo
import android.widget.TextView


class FilterAdapter(val filterStates: List<FilterState>) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {
    val states: Array<Any> = Array(filterStates.size, { 0 })
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterAdapter.ViewHolder {
        return when (viewType) {
            Filter.TYPE_LIST -> ViewHolder(TextSpinner(parent.context))
            Filter.TYPE_TEXT -> ViewHolder(TextEditText(parent.context))
            else -> ViewHolder(ThreeStateCheckBox(parent.context))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filterState = filterStates[position];
        states[position] = filterState.state
        when (holder.view) {
            is ThreeStateCheckBox -> {
                holder.view.position = position
                holder.view.threeState = filterState.filter.type == Filter.TYPE_IGNORE_INCLUDE_EXCLUDE
                holder.view.text = filterState.filter.name
                holder.view.updateButton()
            }
            is TextSpinner -> {
                holder.view.textView.text = filterState.filter.name + ":"
                holder.view.spinner.adapter = ArrayAdapter<Any>(holder.view.context,
                        android.R.layout.simple_spinner_item, filterState.filter.states)
                holder.view.spinner.setSelection(filterState.filter.states.indexOf(filterState.state))
                holder.view.spinner.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, pos: Int, id: Long) {
                        states[position] = filterState.filter.states[pos]
                    }

                    override fun onNothingSelected(parentView: AdapterView<*>) {
                    }
                }
            }
            is TextEditText -> {
                holder.view.textView.text = filterState.filter.name + ":"
                holder.view.editText.setText(filterState.state.toString())
                holder.view.editText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {
                    }

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        states[position] = s
                    }
                })
            }
        }
    }

    override fun getItemCount(): Int {
        return filterStates.size
    }

    override fun getItemViewType(position: Int): Int {
        return filterStates[position].filter.type
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    class TextSpinner(context: Context?) : LinearLayout(context) {
        val textView: TextView = TextView(context)
        val spinner: Spinner = Spinner(context)

        init {
            addView(textView)
            addView(spinner)
        }
    }

    class TextEditText(context: Context?) : LinearLayout(context) {
        val textView: TextView = TextView(context)
        val editText: EditText = EditText(context)

        init {
            addView(textView)
            editText.setSingleLine()
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            addView(editText)
        }
    }

    inner class ThreeStateCheckBox(context: Context?, var position: Int = 0, var threeState: Boolean = false) : CheckBox(context) {
        init {
            setOnCheckedChangeListener { buttonView, isChecked ->
                val all = filterStates[position].filter.states
                states[position] = all[(all.indexOf(states[position]) + 1) % if (threeState) 3 else 2]
                updateButton()
            }
        }

        fun updateButton() {
            setButtonDrawable(VectorDrawableCompat.create(getResources(),
                    arrayOf(R.drawable.ic_check_box_outline_blank_24dp, R.drawable.ic_check_box_24dp,
                            R.drawable.ic_check_box_x_24dp)[filterStates[position].filter.states.indexOf(states[position])],
                    null))
            invalidate()
        }
    }
}