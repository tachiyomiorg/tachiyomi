package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ReaderTransitionViewBinding
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.util.system.isNightMode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ReaderTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val binding: ReaderTransitionViewBinding

    init {
        binding = ReaderTransitionViewBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
    fun bind(transition: ChapterTransition) {
        when (transition) {
            is ChapterTransition.Prev -> bindPrevChapterTransition(transition)
            is ChapterTransition.Next -> bindNextChapterTransition(transition)
        }

        missingChapterWarning(transition)

        val color = when (Injekt.get<PreferencesHelper>().readerTheme().get()) {
            0 -> context.getColor(android.R.color.black)
            3 -> context.getColor(automaticTextColor())
            else -> context.getColor(android.R.color.white)
        }
        listOf(binding.upperText, binding.warningText, binding.lowerText).forEach {
            it.setTextColor(color)
        }
    }

    /**
     * Picks text color for [ReaderActivity] based on light/dark theme preference
     */
    private fun automaticTextColor(): Int {
        return if (context.isNightMode()) {
            android.R.color.white
        } else {
            android.R.color.black
        }
    }

    /**
     * Binds a previous chapter transition on this view and subscribes to the page load status.
     */
    private fun bindPrevChapterTransition(transition: ChapterTransition) {
        val prevChapter = transition.to

        val hasPrevChapter = prevChapter != null
        binding.lowerText.isVisible = hasPrevChapter
        if (hasPrevChapter) {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_TEXT_START
            binding.upperText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_previous)) }
                append("\n${prevChapter!!.chapter.name}")
            }
            binding.lowerText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_current)) }
                append("\n${transition.from.chapter.name}")
            }
        } else {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_CENTER
            binding.upperText.text = context.getString(R.string.transition_no_previous)
        }
    }

    /**
     * Binds a next chapter transition on this view and subscribes to the load status.
     */
    private fun bindNextChapterTransition(transition: ChapterTransition) {
        val nextChapter = transition.to

        val hasNextChapter = nextChapter != null
        binding.lowerText.isVisible = hasNextChapter
        if (hasNextChapter) {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_TEXT_START
            binding.upperText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_finished)) }
                append("\n${transition.from.chapter.name}")
            }
            binding.lowerText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_next)) }
                append("\n${nextChapter!!.chapter.name}")
            }
        } else {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_CENTER
            binding.upperText.text = context.getString(R.string.transition_no_next)
        }
    }

    private fun missingChapterWarning(transition: ChapterTransition) {
        if (transition.to == null) {
            binding.warning.isVisible = false
            return
        }

        val hasMissingChapters = when (transition) {
            is ChapterTransition.Prev -> hasMissingChapters(transition.from, transition.to)
            is ChapterTransition.Next -> hasMissingChapters(transition.to, transition.from)
        }

        if (!hasMissingChapters) {
            binding.warning.isVisible = false
            return
        }

        val chapterDifference = when (transition) {
            is ChapterTransition.Prev -> calculateChapterDifference(transition.from, transition.to)
            is ChapterTransition.Next -> calculateChapterDifference(transition.to, transition.from)
        }

        binding.warningText.text = resources.getQuantityString(R.plurals.missing_chapters_warning, chapterDifference.toInt(), chapterDifference.toInt())
        binding.warning.isVisible = true
    }
}
