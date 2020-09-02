package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

/**
 * View of the ViewPager that contains a chapter transition.
 */
@SuppressLint("ViewConstructor")
class PagerTransitionHolder(
    val viewer: PagerViewer,
    val transition: ChapterTransition
) : LinearLayout(viewer.activity), ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item: Any
        get() = transition

    /**
     * Subscription for status changes of the transition page.
     */
    private var statusSubscription: Subscription? = null

    private var warningImageView: ImageView = ImageView(context).apply {
        val warningDrawable = resources.getDrawable(R.drawable.ic_warning_white_48dp, resources.newTheme())
        background = warningDrawable
        wrapContent()
    }

    private var warningTextView: TextView = TextView(context).apply {
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        layoutParams.bottomMargin = 48
        setLayoutParams(layoutParams)
    }

    /**
     * Text view used to display the text of the current and next/prev chapters.
     */
    private var textView = TextView(context).apply {
        textSize = 17.5F
        wrapContent()
    }

    /**
     * View container of the current status of the transition page. Child views will be added
     * dynamically.
     */
    private var pagesContainer = LinearLayout(context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val sidePadding = 64.dpToPx
        setPadding(sidePadding, 0, sidePadding, 0)
        addView(warningImageView)
        addView(warningTextView)
        addView(textView)
        addView(pagesContainer)

        when (transition) {
            is ChapterTransition.Prev -> bindPrevChapterTransition()
            is ChapterTransition.Next -> bindNextChapterTransition()
        }

        missingChapterWarning()
    }

    private fun missingChapterWarning() {
        if (transition.to == null) {
            showMissingChapterWarning(false)
            return
        }

        val fromChapterNumber: Float = transition.from.chapter.chapter_number
        val toChapterNumber: Float = transition.to!!.chapter.chapter_number

        val chapterDifference = when (transition) {
            is ChapterTransition.Prev -> (fromChapterNumber - toChapterNumber)
            is ChapterTransition.Next -> (toChapterNumber - fromChapterNumber)
        }

        val hasMissingChapters = chapterDifference > 1f

        warningTextView.text = resources.getQuantityString(R.plurals.missing_chapters_warning, chapterDifference.toInt(), chapterDifference.toInt())
        showMissingChapterWarning(hasMissingChapters)
    }

    private fun showMissingChapterWarning(boolean: Boolean) {
        warningImageView.isVisible = boolean
        warningTextView.isVisible = boolean
    }

    /**
     * Called when this view is detached from the window. Unsubscribes any active subscription.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        statusSubscription?.unsubscribe()
        statusSubscription = null
    }

    /**
     * Binds a next chapter transition on this view and subscribes to the load status.
     */
    private fun bindNextChapterTransition() {
        val nextChapter = transition.to

        textView.text = if (nextChapter != null) {
            buildSpannedString {
                bold { append(context.getString(R.string.transition_finished)) }
                append("\n${transition.from.chapter.name}\n\n")
                bold { append(context.getString(R.string.transition_next)) }
                append("\n${nextChapter.chapter.name}\n\n")
            }
        } else {
            context.getString(R.string.transition_no_next)
        }

        if (nextChapter != null) {
            observeStatus(nextChapter)
        }
    }

    /**
     * Binds a previous chapter transition on this view and subscribes to the page load status.
     */
    private fun bindPrevChapterTransition() {
        val prevChapter = transition.to

        textView.text = if (prevChapter != null) {
            buildSpannedString {
                bold { append(context.getString(R.string.transition_current)) }
                append("\n${transition.from.chapter.name}\n\n")
                bold { append(context.getString(R.string.transition_previous)) }
                append("\n${prevChapter.chapter.name}\n\n")
            }
        } else {
            context.getString(R.string.transition_no_previous)
        }

        if (prevChapter != null) {
            observeStatus(prevChapter)
        }
    }

    /**
     * Observes the status of the page list of the next/previous chapter. Whenever there's a new
     * state, the pages container is cleaned up before setting the new state.
     */
    private fun observeStatus(chapter: ReaderChapter) {
        statusSubscription?.unsubscribe()
        statusSubscription = chapter.stateObserver
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                pagesContainer.removeAllViews()
                when (state) {
                    is ReaderChapter.State.Wait -> {
                    }
                    is ReaderChapter.State.Loading -> setLoading()
                    is ReaderChapter.State.Error -> setError(state.error)
                    is ReaderChapter.State.Loaded -> setLoaded()
                }
            }
    }

    /**
     * Sets the loading state on the pages container.
     */
    private fun setLoading() {
        val progress = ProgressBar(context, null, android.R.attr.progressBarStyle)

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            setText(R.string.transition_pages_loading)
        }

        pagesContainer.addView(progress)
        pagesContainer.addView(textView)
    }

    /**
     * Sets the loaded state on the pages container.
     */
    private fun setLoaded() {
        // No additional view is added
    }

    /**
     * Sets the error state on the pages container.
     */
    private fun setError(error: Throwable) {
        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.getString(R.string.transition_pages_error, error.message)
        }

        val retryBtn = PagerButton(context, viewer).apply {
            wrapContent()
            setText(R.string.action_retry)
            setOnClickListener {
                val toChapter = transition.to
                if (toChapter != null) {
                    viewer.activity.requestPreloadChapter(toChapter)
                }
            }
        }

        pagesContainer.addView(textView)
        pagesContainer.addView(retryBtn)
    }

    /**
     * Extension method to set layout params to wrap content on this view.
     */
    private fun View.wrapContent() {
        layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }
}
