package eu.kanade.tachiyomi.widget

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import eu.kanade.tachiyomi.databinding.DownloadCustomAmountBinding
import timber.log.Timber

/**
 * Custom dialog to select how many chapters to download.
 */
class DialogCustomDownloadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    /**
     * Current amount of custom download chooser.
     */
    var amount: Int = 0
        private set

    /**
     * Minimal value of custom download chooser.
     */
    private var min = 0

    /**
     * Maximal value of custom download chooser.
     */
    private var max = 0

    private val binding: DownloadCustomAmountBinding

    init {
        binding = DownloadCustomAmountBinding.inflate(LayoutInflater.from(context), this, false)
        addView(binding.root)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)

        // Set download count to 0.
        binding.myNumber.text = SpannableStringBuilder(getAmount(0).toString())

        // When user presses button decrease amount by 10.
        binding.btnDecrease10.setOnClickListener {
            binding.myNumber.text = SpannableStringBuilder(getAmount(amount - 10).toString())
        }

        // When user presses button increase amount by 10.
        binding.btnIncrease10.setOnClickListener {
            binding.myNumber.text = SpannableStringBuilder(getAmount(amount + 10).toString())
        }

        // When user presses button decrease amount by 1.
        binding.btnDecrease.setOnClickListener {
            binding.myNumber.text = SpannableStringBuilder(getAmount(amount - 1).toString())
        }

        // When user presses button increase amount by 1.
        binding.btnIncrease.setOnClickListener {
            binding.myNumber.text = SpannableStringBuilder(getAmount(amount + 1).toString())
        }

        // When user inputs custom number set amount equal to input.
        binding.myNumber.doOnTextChanged { text, _, _, _ ->
            try {
                amount = getAmount(text.toString().toInt())
            } catch (error: NumberFormatException) {
                // Catch NumberFormatException to prevent parse exception when input is empty.
                Timber.e(error)
            }
        }
    }

    /**
     * Set min max of custom download amount chooser.
     * @param min minimal downloads
     * @param max maximal downloads
     */
    fun setMinMax(min: Int, max: Int) {
        this.min = min
        this.max = max
    }

    /**
     * Returns amount to download.
     * if minimal downloads is less than input return minimal downloads.
     * if Maximal downloads is more than input return maximal downloads.
     *
     * @return amount to download.
     */
    private fun getAmount(input: Int): Int {
        return when {
            input > max -> max
            input < min -> min
            else -> input
        }
    }
}
