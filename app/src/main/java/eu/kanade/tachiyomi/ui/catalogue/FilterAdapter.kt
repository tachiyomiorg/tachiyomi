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
import android.text.TextWatcher
import android.text.Editable
import android.view.inputmethod.EditorInfo
import android.widget.TextView


class FilterAdapter(val filters: List<Filter<*>>) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {
    private companion object {
        const val CHECKBOX = 0
        const val TRISTATE = 1
        const val LIST = 2
        const val TEXT = 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterAdapter.ViewHolder {
        return when (viewType) {
            LIST -> ViewHolder(TextSpinner(parent.context))
            TEXT -> ViewHolder(TextEditText(parent.context))
            else -> ViewHolder(CheckBox(parent.context))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filter = filters[position]
        when (filter) {
            is Filter.CheckBox -> {
                var checkBox = holder.view as CheckBox
                checkBox.text = filter.name
                checkBox.isChecked = filter.state
                val unCheckIcon = VectorDrawableCompat.create(checkBox.getResources(), R.drawable.ic_check_box_outline_blank_24dp, null)
                val checkIcon = VectorDrawableCompat.create(checkBox.getResources(), R.drawable.ic_check_box_24dp, null)
                checkBox.setButtonDrawable(if (filter.state) checkIcon else unCheckIcon)
                checkBox.invalidate()
                checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    filter.state = isChecked
                    checkBox.setButtonDrawable(if (isChecked) checkIcon else unCheckIcon)
                    checkBox.invalidate()
                }
            }
            is Filter.TriState -> {
                var triCheckBox = holder.view as CheckBox
                triCheckBox.text = filter.name
                val icons = arrayOf(VectorDrawableCompat.create(triCheckBox.getResources(), R.drawable.ic_check_box_outline_blank_24dp, null),
                        VectorDrawableCompat.create(triCheckBox.getResources(), R.drawable.ic_check_box_24dp, null),
                        VectorDrawableCompat.create(triCheckBox.getResources(), R.drawable.ic_check_box_x_24dp, null))
                triCheckBox.setButtonDrawable(icons[filter.state])
                triCheckBox.invalidate()
                triCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    filter.state = (filter.state + 1) % 3
                    triCheckBox.setButtonDrawable(icons[filter.state])
                    triCheckBox.invalidate()
                }
            }
            is Filter.List<*> -> {
                var txtSpin = holder.view as TextSpinner
                txtSpin.textView.text = filter.name + ":"
                txtSpin.spinner.adapter = ArrayAdapter<Any>(holder.view.context,
                        android.R.layout.simple_spinner_item, filter.values)
                txtSpin.spinner.setSelection(filter.state)
                txtSpin.spinner.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, pos: Int, id: Long) {
                        filter.state = pos
                    }

                    override fun onNothingSelected(parentView: AdapterView<*>) {
                    }
                }
            }
            is Filter.Text -> {
                var txtEdTx = holder.view as TextEditText
                txtEdTx.textView.text = filter.name + ":"
                txtEdTx.editText.setText(filter.state)
                txtEdTx.editText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {
                    }

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        filter.state = s.toString()
                    }
                })
            }
        }
    }

    override fun getItemCount(): Int {
        return filters.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (filters[position]) {
            is Filter.CheckBox -> CHECKBOX
            is Filter.TriState -> TRISTATE
            is Filter.List<*> -> LIST
            is Filter.Text -> TEXT
        }
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    private class TextSpinner(context: Context?) : LinearLayout(context) {
        val textView: TextView = TextView(context)
        val spinner: Spinner = Spinner(context)

        init {
            addView(textView)
            addView(spinner)
        }
    }

    private class TextEditText(context: Context?) : LinearLayout(context) {
        val textView: TextView = TextView(context)
        val editText: EditText = EditText(context)

        init {
            addView(textView)
            editText.setSingleLine()
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            addView(editText)
        }
    }
}