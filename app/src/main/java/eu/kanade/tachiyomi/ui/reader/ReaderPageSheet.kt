package eu.kanade.tachiyomi.ui.reader

import android.os.Bundle
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderPageSheetBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/**
 * Sheet to show when a page is long clicked.
 */
class ReaderPageSheet(
    private val activity: ReaderActivity,
    private val page: ReaderPage
) : BottomSheetDialog(activity) {

    private val binding = ReaderPageSheetBinding.inflate(activity.layoutInflater, null, false)

    init {
        setContentView(binding.root)

        binding.setAsCoverLayout.setOnClickListener { setAsCover() }
        binding.shareLayout.setOnClickListener { share() }
        binding.saveLayout.setOnClickListener { save() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
        if (width > 0) {
            window?.setLayout(width, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    /**
     * Sets the image of this page as the cover of the manga.
     */
    private fun setAsCover() {
        if (page.status != Page.READY) return

        MaterialDialog(activity)
            .message(R.string.confirm_set_image_as_cover)
            .positiveButton(android.R.string.ok) {
                activity.setAsCover(page)
                dismiss()
            }
            .negativeButton(android.R.string.cancel)
            .show()
    }

    /**
     * Shares the image of this page with external apps.
     */
    private fun share() {
        activity.shareImage(page)
        dismiss()
    }

    /**
     * Saves the image of this page on external storage.
     */
    private fun save() {
        activity.saveImage(page)
        dismiss()
    }
}
