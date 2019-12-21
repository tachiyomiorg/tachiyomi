package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.graphics.Typeface
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.AppCompatTextView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.dpToPx
import eu.kanade.tachiyomi.util.visibleIf
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

/**
 * Holder of the webtoon viewer that contains a chapter transition.
 */
class WebtoonTransitionHolder(
        val layout: LinearLayout,
        viewer: WebtoonViewer
) : WebtoonBaseHolder(layout, viewer) {

    /**
     * Subscription for status changes of the transition page.
     */
    private var statusSubscription: Subscription? = null

    /**
     * Text view used to display the text of the current and next/prev chapters.
     */
    private var textView = TextView(context)

    /**
     * View container of the current status of the transition page. Child views will be added
     * dynamically.
     */
    private var pagesContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
    }

    init {
        layout.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val paddingVertical = 48.dpToPx
        val paddingHorizontal = 32.dpToPx
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

        val childMargins = 16.dpToPx
        val childParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(0, childMargins, 0, childMargins)
        }

        layout.addView(textView, childParams)
        layout.addView(pagesContainer, childParams)
    }

    /**
     * Binds the given [transition] with this view holder, subscribing to its state.
     */
    fun bind(transition: ChapterTransition) {
        when (transition) {
            is ChapterTransition.Prev -> bindPrevChapterTransition(transition)
            is ChapterTransition.Next -> bindNextChapterTransition(transition)
        }
    }

    /**
     * Called when the view is recycled and being added to the view pool.
     */
    override fun recycle() {
        unsubscribeStatus()
    }

    /**
     * Binds a next chapter transition on this view and subscribes to the load status.
     */
    private fun bindNextChapterTransition(transition: ChapterTransition.Next) {
        val nextChapter = transition.to

        textView.text = if (nextChapter != null) {
            SpannableStringBuilder().apply {
                append(context.getString(R.string.transition_finished))
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                append("\n${transition.from.chapter.name}\n\n")
                val currSize = length
                append(context.getString(R.string.transition_next))
                setSpan(StyleSpan(Typeface.BOLD), currSize, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                append("\n${nextChapter.chapter.name}\n\n")
            }
        } else {
            context.getString(R.string.transition_no_next)
        }

        if (nextChapter != null) {
            observeStatus(nextChapter, transition)
        }
    }

    /**
     * Binds a previous chapter transition on this view and subscribes to the page load status.
     */
    private fun bindPrevChapterTransition(transition: ChapterTransition.Prev) {
        val prevChapter = transition.to

        textView.text = if (prevChapter != null) {
            SpannableStringBuilder().apply {
                append(context.getString(R.string.transition_current))
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                append("\n${transition.from.chapter.name}\n\n")
                val currSize = length
                append(context.getString(R.string.transition_previous))
                setSpan(StyleSpan(Typeface.BOLD), currSize, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                append("\n${prevChapter.chapter.name}\n\n")
            }
        } else {
            context.getString(R.string.transition_no_previous)
        }

        if (prevChapter != null) {
            observeStatus(prevChapter, transition)
        }
    }

    /**
     * Observes the status of the page list of the next/previous chapter. Whenever there's a new
     * state, the pages container is cleaned up before setting the new state.
     */
    private fun observeStatus(chapter: ReaderChapter, transition: ChapterTransition) {
        unsubscribeStatus()

        statusSubscription = chapter.stateObserver
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                pagesContainer.removeAllViews()
                when (state) {
                    is ReaderChapter.State.Wait -> {}
                    is ReaderChapter.State.Loading -> setLoading()
                    is ReaderChapter.State.Error -> setError(state.error, transition)
                    is ReaderChapter.State.Loaded -> setLoaded()
                }
                pagesContainer.visibleIf { pagesContainer.childCount > 0 }
            }

        addSubscription(statusSubscription)
    }

    /**
     * Unsubscribes from the status subscription.
     */
    private fun unsubscribeStatus() {
        removeSubscription(statusSubscription)
        statusSubscription = null
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
    private fun setError(error: Throwable, transition: ChapterTransition) {
        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.getString(R.string.transition_pages_error, error.message)
        }

        val retryBtn = AppCompatButton(context).apply {
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

}
