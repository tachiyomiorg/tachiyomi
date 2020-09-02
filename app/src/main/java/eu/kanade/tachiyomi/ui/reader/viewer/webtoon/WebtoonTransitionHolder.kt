package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlin.math.floor
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Holder of the webtoon viewer that contains a chapter transition.
 */
class WebtoonTransitionHolder(
    val layout: LinearLayout,
    viewer: WebtoonViewer,
    val preferences: PreferencesHelper = Injekt.get()
) : WebtoonBaseHolder(layout, viewer) {

    /**
     * Subscription for status changes of the transition page.
     */
    private var statusSubscription: Subscription? = null

    private var warningImageView: ImageView = ImageView(context).apply {
        val warningDrawable = resources.getDrawable(R.drawable.ic_warning_white_48dp, resources.newTheme())
        background = warningDrawable
        val tintColor = when (preferences.readerTheme().get()) {
            0 -> Color.BLACK // Theme is White
            else -> Color.WHITE // Theme is Black or Gray
        }
        backgroundTintList = ColorStateList.valueOf(tintColor)
        wrapContent()
    }

    private var warningTextView: TextView = TextView(context).apply {
        val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.bottomMargin = 16.dpToPx
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

        layout.addView(warningImageView)
        layout.addView(warningTextView)
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

        missingChapterWarning(transition)
    }

    private fun missingChapterWarning(transition: ChapterTransition) {
        if (transition.to == null) {
            showMissingChapterWarning(false)
            return
        }

        val fromChapterNumber: Float = floor(transition.from.chapter.chapter_number)
        val toChapterNumber: Float = floor(transition.to!!.chapter.chapter_number)

        val chapterDifference = when (transition) {
            is ChapterTransition.Prev -> fromChapterNumber - toChapterNumber - 1f
            is ChapterTransition.Next -> toChapterNumber - fromChapterNumber - 1f
        }

        val hasMissingChapters = chapterDifference > 0f

        warningTextView.text = itemView.resources.getQuantityString(R.plurals.missing_chapters_warning, chapterDifference.toInt(), chapterDifference.toInt())
        showMissingChapterWarning(hasMissingChapters)
    }

    private fun showMissingChapterWarning(boolean: Boolean) {
        warningImageView.isVisible = boolean
        warningTextView.isVisible = boolean
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
            observeStatus(nextChapter, transition)
        }
    }

    /**
     * Binds a previous chapter transition on this view and subscribes to the page load status.
     */
    private fun bindPrevChapterTransition(transition: ChapterTransition.Prev) {
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
                    is ReaderChapter.State.Wait -> {
                    }
                    is ReaderChapter.State.Loading -> setLoading()
                    is ReaderChapter.State.Error -> setError(state.error, transition)
                    is ReaderChapter.State.Loaded -> setLoaded()
                }
                pagesContainer.isVisible = pagesContainer.isNotEmpty()
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
