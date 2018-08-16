package eu.kanade.tachiyomi.ui.reader

import android.support.design.widget.BottomSheetDialog
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.android.synthetic.main.reader_page_sheet.*

class ReaderPageSheet(
        private val activity: ReaderActivity,
        private val page: ReaderPage
) : BottomSheetDialog(activity) {

    val view = activity.layoutInflater.inflate(R.layout.reader_page_sheet, null)

    init {
        setContentView(view)

        set_as_cover_layout.setOnClickListener { setAsCover() }
        share_layout.setOnClickListener { share() }
        save_layout.setOnClickListener { save() }
    }

    private fun setAsCover() {
        if (page.status != Page.READY) return

        MaterialDialog.Builder(activity)
            .content(activity.getString(R.string.confirm_set_image_as_cover))
            .positiveText(android.R.string.yes)
            .negativeText(android.R.string.no)
            .onPositive { _, _ ->
                activity.setAsCover(page)
                dismiss()
            }
            .show()
    }

    private fun share() {
        activity.shareImage(page)
        dismiss()
    }

    private fun save() {
        activity.saveImage(page)
        dismiss()
    }

}
