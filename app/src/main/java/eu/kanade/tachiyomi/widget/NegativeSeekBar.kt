package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import eu.kanade.tachiyomi.R


class NegativeSeekBar @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
        SeekBar(context, attrs) {

    private var minValue : Int = 0
    private var maxValue : Int = 0
    private var listener : OnSeekBarChangeListener? = null

    init {
        val styledAttributes = getContext().obtainStyledAttributes(
                attrs,
                R.styleable.NegativeSeekBar,0,0)

        try {
            setMinSeek(styledAttributes.getInt(R.styleable.NegativeSeekBar_min_seek, 0))
            setMaxSeek(styledAttributes.getInt(R.styleable.NegativeSeekBar_max_seek, 0))
        } finally {
            styledAttributes.recycle()
        }

        super.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                listener?.let { it.onProgressChanged(seekBar,minValue + value, fromUser) }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                listener?.let { it.onStartTrackingTouch(p0) }
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                listener?.let { it.onStopTrackingTouch(p0) }
            }
        })
    }

    fun setMinSeek(minValue: Int) {
        this@NegativeSeekBar.minValue = minValue
        super.setMax(this@NegativeSeekBar.maxValue - this@NegativeSeekBar.minValue)
    }

    fun setMaxSeek(maxValue: Int) {
        this@NegativeSeekBar.maxValue = maxValue
        super.setMax(this@NegativeSeekBar.maxValue -  this@NegativeSeekBar.minValue)
    }

    override fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener?) {
        this.listener = listener
    }

}