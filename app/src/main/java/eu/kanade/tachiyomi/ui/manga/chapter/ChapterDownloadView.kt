package eu.kanade.tachiyomi.ui.manga.chapter

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.ChapterDownloadViewBinding
import eu.kanade.tachiyomi.util.view.setVectorCompat

class ChapterDownloadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val binding: ChapterDownloadViewBinding =
        ChapterDownloadViewBinding.inflate(LayoutInflater.from(context), this, false)

    private var state = Download.State.NOT_DOWNLOADED
    private var progress = 0

    private var downloadIconAnimator: ObjectAnimator? = null

    init {
        addView(binding.root)
    }

    fun setState(state: Download.State, progress: Int = 0) {
        val isDirty = this.state.value != state.value || this.progress != progress
        if (isDirty) {
            updateLayout(state, progress)
        }
    }

    private fun updateLayout(state: Download.State, progress: Int) {
        binding.downloadIcon.isVisible = state == Download.State.NOT_DOWNLOADED ||
            state == Download.State.DOWNLOADING || state == Download.State.QUEUE
        if (state == Download.State.DOWNLOADING || state == Download.State.QUEUE) {
            if (downloadIconAnimator == null) {
                downloadIconAnimator =
                    ObjectAnimator.ofFloat(binding.downloadIcon, "alpha", 1f, 0f).apply {
                        duration = 1000
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.REVERSE
                    }
                downloadIconAnimator?.start()
            }
            downloadIconAnimator?.currentPlayTime = System.currentTimeMillis() % 2000
        } else if (downloadIconAnimator != null) {
            downloadIconAnimator?.cancel()
            downloadIconAnimator = null
            binding.downloadIcon.alpha = 1f
        }

        binding.downloadProgress.isVisible = state == Download.State.DOWNLOADING ||
            state == Download.State.NOT_DOWNLOADED || state == Download.State.QUEUE
        if (state == Download.State.DOWNLOADING) {
            binding.downloadProgress.setProgressCompat(progress, true)
        } else {
            binding.downloadProgress.setProgressCompat(100, true)
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
