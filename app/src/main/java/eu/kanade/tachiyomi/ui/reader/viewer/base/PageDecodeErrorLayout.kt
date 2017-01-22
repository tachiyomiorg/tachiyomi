package eu.kanade.tachiyomi.ui.reader.viewer.base

import android.net.Uri
import android.support.v4.content.ContextCompat
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.android.synthetic.main.page_decode_error.view.*

class PageDecodeErrorLayout(
        val view: View,
        val page: Page,
        val theme: Int,
        val retryListener: () -> Unit
) {

    init {
        val textColor = if (theme == ReaderActivity.BLACK_THEME)
            ContextCompat.getColor(view.context, R.color.textColorSecondaryDark)
        else
            ContextCompat.getColor(view.context, R.color.textColorSecondaryLight)

        view.decode_error_text.setTextColor(textColor)

        view.decode_retry.setOnClickListener {
            retryListener()
        }

        view.decode_open_browser.setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(page.imageUrl))
            view.context.startActivity(intent)
        }

        if (page.imageUrl == null) {
            view.decode_open_browser.visibility = View.GONE
        }
    }

}
