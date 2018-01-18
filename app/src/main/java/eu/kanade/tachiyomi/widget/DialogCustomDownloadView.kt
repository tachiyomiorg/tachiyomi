package eu.kanade.tachiyomi.widget

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.inflate
import kotlinx.android.synthetic.main.navigation_download_chooser.view.*


class DialogCustomDownloadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        LinearLayout(context, attrs) {

    var amount: Int = 0
        private set

    private var min = 0
    private var max = 10

    init {
        addView(inflate(R.layout.navigation_download_chooser))

    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        btn_decrease_10.setOnClickListener {
            myNumber.text = SpannableStringBuilder(getAmount(amount - 10).toString())
        }
        btn_increase_10.setOnClickListener {
            myNumber.text = SpannableStringBuilder(getAmount(amount + 10).toString())
        }
        btn_decrease.setOnClickListener {
            myNumber.text = SpannableStringBuilder(getAmount(amount - 1).toString())
        }
        btn_increase.setOnClickListener {
            myNumber.text = SpannableStringBuilder(getAmount(amount + 1).toString())
        }
        myNumber.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    amount = getAmount(Integer.parseInt(s.toString()))
                } catch (error: NumberFormatException) {
                    amount = 0
                }
            }
        })
    }

    fun setMinMax(min: Int, max: Int) {
        this.min = min
        this.max = max
    }

    private fun getAmount(input: Int): Int {
        return when {
            input > max -> max
            input < min -> min
            else -> input
        }
    }
}
