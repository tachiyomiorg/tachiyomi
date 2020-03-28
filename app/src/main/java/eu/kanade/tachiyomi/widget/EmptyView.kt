package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlin.random.Random
import kotlinx.android.synthetic.main.common_view_empty.view.text_face
import kotlinx.android.synthetic.main.common_view_empty.view.text_label

class EmptyView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        RelativeLayout(context, attrs) {

    init {
        inflate(context, R.layout.common_view_empty, this)
    }

    /**
     * Hide the information view
     */
    fun hide() {
        this.gone()
    }

    /**
     * Show the information view
     * @param textResource text of information view
     */
    fun show(@StringRes textResource: Int) {
        text_face.text = getRandomErrorFace()
        text_label.text = context.getString(textResource)
        this.visible()
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
}
