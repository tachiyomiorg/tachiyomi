package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.support.v7.widget.AppCompatTextView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.dpToPx
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

@SuppressLint("ViewConstructor")
class PagerTransitionHolder(
        val viewer: PagerViewer,
        val transition: ChapterTransition
) : LinearLayout(viewer.activity), ViewPagerAdapter.PositionableView {

    override val item: Any
        get() = transition

    private var statusSubscription: Subscription? = null

    private var textView = TextView(context).apply {
        wrapContent()
    }

    private var pagesContainer = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val sidePadding = 64.dpToPx
        setPadding(sidePadding, 0, sidePadding, 0)
        addView(textView)
        addView(pagesContainer)

        when (transition) {
            is ChapterTransition.Prev -> bindPrevChapterTransition()
            is ChapterTransition.Next -> bindNextChapterTransition()
        }
    }

    private fun bindNextChapterTransition() {
        val nextChapter = transition.to

        textView.text = if (nextChapter != null) {
            "Finished reading ${transition.from.chapter.name}.\n\n" +
            "Next chapter: ${nextChapter.chapter.name}\n\n"
        } else {
            "There's no next chapter"
        }

        if (nextChapter != null) {
            observeStatus(nextChapter)
        }
    }

    private fun bindPrevChapterTransition() {
        val prevChapter = transition.to

        textView.text = if (prevChapter != null) {
            "Beginning of ${transition.from.chapter.name}.\n\n" +
            "Previous chapter: ${prevChapter.chapter.name}\n\n"
        } else {
            "There's no previous chapter"
        }

        if (prevChapter != null) {
            observeStatus(prevChapter)
        }
    }

    private fun observeStatus(chapter: ReaderChapter) {
        statusSubscription?.unsubscribe()
        statusSubscription = chapter.stateObserver
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                pagesContainer.removeAllViews()
                when (state) {
                    is ReaderChapter.State.Wait -> {}
                    is ReaderChapter.State.Loading -> setLoading()
                    is ReaderChapter.State.Error -> setError(state.error)
                    is ReaderChapter.State.Loaded -> setLoaded()
                }
            }
    }

    private fun setLoading() {
        val progress = ProgressBar(context, null, android.R.attr.progressBarStyle)

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = "Loading pages..."
        }

        pagesContainer.addView(progress)
        pagesContainer.addView(textView)
    }

    private fun setLoaded() {

    }

    private fun setError(error: Throwable) {
        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = "Failed to load pages: ${error.message}"
        }

        val retryBtn = PagerButton(context, viewer).apply {
            wrapContent()
            setText(R.string.action_retry)
            setOnClickListener {
                if (transition is ChapterTransition.Next) {
                    viewer.activity.requestPreloadNextChapter()
                } else {
                    viewer.activity.requestPreloadPreviousChapter()
                }
            }
        }

        pagesContainer.addView(textView)
        pagesContainer.addView(retryBtn)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        statusSubscription?.unsubscribe()
        statusSubscription = null
    }

    private fun View.wrapContent() {
        layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

}
