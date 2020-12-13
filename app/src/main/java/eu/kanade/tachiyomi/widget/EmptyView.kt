package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.databinding.CommonViewEmptyBinding
import kotlin.random.Random

class EmptyView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    RelativeLayout(context, attrs) {

    private val binding: CommonViewEmptyBinding

    init {
        binding = CommonViewEmptyBinding.inflate(LayoutInflater.from(context), this, true)
    }

    /**
     * Hide the information view
     */
    fun hide() {
        this.isVisible = false
    }

    /**
     * Show the information view
     * @param textResource text of information view
     */
    fun show(@StringRes textResource: Int, actions: List<Action>? = null) {
        show(context.getString(textResource), actions)
    }

    fun show(message: String, actions: List<Action>? = null) {
        binding.textFace.text = getRandomErrorFace()
        binding.textLabel.text = message

        binding.actionsContainer.removeAllViews()
        if (!actions.isNullOrEmpty()) {
            actions.forEach {
                val button = AppCompatButton(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    setText(it.resId)
                    setOnClickListener(it.listener)
                }

                binding.actionsContainer.addView(button)
            }
        }

        this.isVisible = true
    }

    companion object {
        private val ERROR_FACES = listOf(
            "(･o･;)",
            "Σ(ಠ_ಠ)",
            "ಥ_ಥ",
            "(˘･_･˘)",
            "(；￣Д￣)",
            "(･Д･。"
        )

        fun getRandomErrorFace(): String {
            return ERROR_FACES[Random.nextInt(ERROR_FACES.size)]
        }
    }

    data class Action(
        @StringRes val resId: Int,
        val listener: OnClickListener
    )
}
