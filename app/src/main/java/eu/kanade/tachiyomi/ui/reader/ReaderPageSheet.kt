package eu.kanade.tachiyomi.ui.reader

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.android.synthetic.main.reader_extract_page_text_sheet.view.*
import kotlinx.android.synthetic.main.reader_page_sheet.*
import kotlinx.android.synthetic.main.reader_page_sheet.view.*
import timber.log.Timber

/**
 * Sheet to show when a page is long clicked.
 */
class ReaderPageSheet(
    private val activity: ReaderActivity,
    private val page: ReaderPage
) : BottomSheetDialog(activity) {

    private val view = activity.layoutInflater.inflate(R.layout.reader_page_sheet, null)

    init {
        setContentView(view)

        extract_text_from_page.setOnClickListener { extractTextFromImage() }
        set_as_cover_layout.setOnClickListener { setAsCover() }
        share_layout.setOnClickListener { share() }
        save_layout.setOnClickListener { save() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
        if (width > 0) {
            window?.setLayout(width, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    /**
     * Extracts the text from the image of this page and displays it to the user.
     */
    private fun extractTextFromImage() {
        try {
            val stream = page.stream!!
            val bmp = BitmapFactory.decodeStream(stream())
            val image = FirebaseVisionImage.fromBitmap(bmp)

            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

            detector.processImage(image)
                .addOnSuccessListener { result ->
                    // Task completed successfully
                    // ...
                    val resultText = result.text
                    Timber.d("resultText: $resultText")

                    ReaderExtractedTextSheet(activity, resultText).show()

                    this@ReaderPageSheet.dismiss()
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                }
        } catch (ex: Throwable) {
            Timber.e(ex.message)
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
