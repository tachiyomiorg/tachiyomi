package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.View
import android.widget.TextView
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.util.dpToPx

class WebtoonTransitionHolder(val view: View) : BaseViewHolder(view) {

    private val textView = view as TextView

    init {
        val paddings = 64.dpToPx
        textView.setPadding(0, paddings, 0, paddings)
    }

    fun bind(transition: ChapterTransition) {
        textView.text = transition.toString()
    }
}
