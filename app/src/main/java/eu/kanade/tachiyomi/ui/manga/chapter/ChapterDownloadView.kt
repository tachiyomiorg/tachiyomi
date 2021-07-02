package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.mikepenz.aboutlibraries.util.getThemeColor
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.ChapterDownloadViewBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.setVectorCompat

class ChapterDownloadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val binding: ChapterDownloadViewBinding =
        ChapterDownloadViewBinding.inflate(LayoutInflater.from(context), this, false)

    private var state: Download.State? = null
    private var progress = -1

    init {
        addView(binding.root)
    }

    fun setState(state: Download.State, progress: Int = -1) {
        val isDirty = this.state?.value != state.value || this.progress != progress
        if (isDirty) {
            updateLayout(state, progress)
        }
    }

    private fun updateLayout(state: Download.State, progress: Int) {
        binding.downloadIcon.isVisible = state == Download.State.NOT_DOWNLOADED ||
            state == Download.State.DOWNLOADING || state == Download.State.QUEUE
        binding.downloadIcon.imageTintList = if (state == Download.State.DOWNLOADING && progress > 0) {
            ColorStateList.valueOf(context.getThemeColor(android.R.attr.colorBackground))
        } else {
            ColorStateList.valueOf(context.getThemeColor(android.R.attr.textColorHint))
        }

        binding.downloadProgress.apply {
            val shouldBeVisible = state == Download.State.DOWNLOADING ||
                state == Download.State.NOT_DOWNLOADED || state == Download.State.QUEUE
            if (shouldBeVisible) {
                hideAnimationBehavior = BaseProgressIndicator.HIDE_NONE
                if (state == Download.State.NOT_DOWNLOADED || state == Download.State.QUEUE) {
                    trackThickness = 2.dpToPx
                    setIndicatorColor(context.getThemeColor(android.R.attr.textColorHint))
                    if (state == Download.State.NOT_DOWNLOADED) {
                        if (isIndeterminate) {
                            hide()
                            isIndeterminate = false
                        }
                        setProgressCompat(100, false)
                    } else if (!isIndeterminate) {
                        hide()
                        isIndeterminate = true
                        show()
                    }
                } else if (state == Download.State.DOWNLOADING) {
                    if (isIndeterminate) {
                        hide()
                    }
                    trackThickness = 12.dpToPx
                    setIndicatorColor(context.getThemeColor(android.R.attr.textColorPrimary))
                    setProgressCompat(progress, true)
                }
                show()
            } else {
                hideAnimationBehavior = BaseProgressIndicator.HIDE_OUTWARD
                hide()
            }
        }

        binding.downloadStatusIcon.apply {
            if (state == Download.State.DOWNLOADED || state == Download.State.ERROR) {
                isVisible = true
                if (state == Download.State.DOWNLOADED) {
                    setVectorCompat(R.drawable.ic_check_circle_24dp, android.R.attr.textColorPrimary)
                } else {
                    setVectorCompat(R.drawable.ic_error_outline_24dp, R.attr.colorError)
                }
            } else {
                isVisible = false
            }
        }

        this.state = state
        this.progress = progress
    }
}
