package eu.kanade.tachiyomi.widget

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import eu.kanade.tachiyomi.R
import kotlin.math.abs

class NegativeSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatSeekBar(context, attrs) {

    private var minValue: Int = 0
    private var maxValue: Int = 0
    private var listener: OnSeekBarChangeListener? = null

    init {
        val styledAttributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.NegativeSeekBar,
            0,
            0
        )

        try {
            setMinSeek(styledAttributes.getInt(R.styleable.NegativeSeekBar_min_seek, 0))
            setMaxSeek(styledAttributes.getInt(R.styleable.NegativeSeekBar_max_seek, 0))
        } finally {
            styledAttributes.recycle()
        }

        super.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    listener?.onProgressChanged(seekBar, minValue + value, fromUser)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    listener?.onStartTrackingTouch(p0)
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    listener?.onStopTrackingTouch(p0)
                }
            }
        )
    }

    override fun setProgress(progress: Int) {
        super.setProgress(abs(minValue) + progress)
    }

    fun setMinSeek(minValue: Int) {
        this.minValue = minValue
        max = (this.maxValue - this.minValue)
    }

    fun setMaxSeek(maxValue: Int) {
        this.maxValue = maxValue
        max = (this.maxValue - this.minValue)
    }

    override fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener?) {
        this.listener = listener
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // We can't restore the progress from the saved state because it gets shifted.
        val origProgress = progress
        super.onRestoreInstanceState(state)
        super.setProgress(origProgress)
    }
}
