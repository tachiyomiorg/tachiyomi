package eu.kanade.tachiyomi.ui.reader

import android.view.LayoutInflater
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderPageSheetBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.widget.sheet.BaseBottomSheetDialog

/**
 * Sheet to show when a page is long clicked.
 */
class ReaderPageSheet(
    private val activity: ReaderActivity,
    private val page: ReaderPage
) : BaseBottomSheetDialog(activity) {

    private lateinit var binding: ReaderPageSheetBinding

    override fun createView(inflater: LayoutInflater): View {
        binding = ReaderPageSheetBinding.inflate(activity.layoutInflater, null, false)

        binding.setAsCoverLayout.setOnClickListener { setAsCover() }
        binding.shareLayout.setOnClickListener { share() }
        binding.saveLayout.setOnClickListener { save() }

        return binding.root
    }

    /**
     * Sets the image of this page as the cover of the manga.
     */
    private fun setAsCover() {
        if (page.status != Page.READY) return

        MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.confirm_set_image_as_cover)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                activity.setAsCover(page)
            }
            .setNegativeButton(android.R.string.cancel, null)
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
