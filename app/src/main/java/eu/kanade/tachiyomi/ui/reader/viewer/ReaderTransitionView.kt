package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import kotlinx.android.synthetic.main.reader_transition_view.view.lower_text
import kotlinx.android.synthetic.main.reader_transition_view.view.upper_text
import kotlinx.android.synthetic.main.reader_transition_view.view.warning
import kotlinx.android.synthetic.main.reader_transition_view.view.warning_text
import kotlin.math.floor

class ReaderTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.reader_transition_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(transition: ChapterTransition) {
        when (transition) {
            is ChapterTransition.Prev -> bindPrevChapterTransition(transition)
            is ChapterTransition.Next -> bindNextChapterTransition(transition)
        }

        missingChapterWarning(transition)
    }

    /**
     * Binds a previous chapter transition on this view and subscribes to the page load status.
     */
    private fun bindPrevChapterTransition(transition: ChapterTransition) {
        val prevChapter = transition.to

        val hasPrevChapter = prevChapter != null
        lower_text.isVisible = hasPrevChapter
        if (hasPrevChapter) {
            upper_text.textAlignment = TEXT_ALIGNMENT_TEXT_START
            upper_text.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_current)) }
                append("\n${transition.from.chapter.name}")
            }
            lower_text.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_previous)) }
                append("\n${prevChapter!!.chapter.name}")
            }
        } else {
            upper_text.textAlignment = TEXT_ALIGNMENT_CENTER
            upper_text.text = context.getString(R.string.transition_no_previous)
        }
    }

    /**
     * Binds a next chapter transition on this view and subscribes to the load status.
     */
    private fun bindNextChapterTransition(transition: ChapterTransition) {
        val nextChapter = transition.to

        val hasNextChapter = nextChapter != null
        lower_text.isVisible = hasNextChapter
        if (hasNextChapter) {
            upper_text.textAlignment = TEXT_ALIGNMENT_TEXT_START
            upper_text.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_finished)) }
                append("\n${transition.from.chapter.name}")
            }
            lower_text.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_next)) }
                append("\n${nextChapter!!.chapter.name}")
            }
        } else {
            upper_text.textAlignment = TEXT_ALIGNMENT_CENTER
            upper_text.text = context.getString(R.string.transition_no_next)
        }
    }

    private fun missingChapterWarning(transition: ChapterTransition) {
        if (transition.to == null) {
            warning.isVisible = false
            return
        }

        val fromChapterNumber: Float = floor(transition.from.chapter.chapter_number)
        val toChapterNumber: Float = floor(transition.to!!.chapter.chapter_number)

        val chapterDifference = when (transition) {
            is ChapterTransition.Prev -> fromChapterNumber - toChapterNumber - 1f
            is ChapterTransition.Next -> toChapterNumber - fromChapterNumber - 1f
        }

        val hasMissingChapters = when (transition) {
            is ChapterTransition.Prev -> MissingChapters.hasMissingChapters(fromChapterNumber, toChapterNumber)
            is ChapterTransition.Next -> MissingChapters.hasMissingChapters(toChapterNumber, fromChapterNumber)
        }

        warning_text.text = resources.getQuantityString(R.plurals.missing_chapters_warning, chapterDifference.toInt(), chapterDifference.toInt())
        warning.isVisible = hasMissingChapters
    }
}
