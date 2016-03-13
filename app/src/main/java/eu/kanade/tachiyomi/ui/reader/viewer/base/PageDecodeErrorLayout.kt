package eu.kanade.tachiyomi.ui.reader.viewer.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderActivity

class PageDecodeErrorLayout(context: Context) : LinearLayout(context) {

    private val lightGreyColor = ContextCompat.getColor(context, android.R.attr.textColorHint)
    private val blackColor = ContextCompat.getColor(context, android.R.attr.textColorPrimary)

    init {
        orientation = LinearLayout.VERTICAL
        setGravity(Gravity.CENTER)
    }

    constructor(context: Context, page: Page, theme: Int, retryListener: () -> Unit) : this(context) {

        // Error message.
        TextView(context).apply {
            gravity = Gravity.CENTER
            setText(R.string.decode_image_error)
            setTextColor(if (theme == ReaderActivity.BLACK_THEME) lightGreyColor else blackColor)
            addView(this)
        }

        // Retry button.
        Button(context).apply {
            layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setText(R.string.action_retry)
            setOnClickListener {
                removeAllViews()
                retryListener()
            }
            addView(this)
        }

        // Open in browser button.
        Button(context).apply {
            layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setText(R.string.action_open_in_browser)
            setOnClickListener { v ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(page.imageUrl))
                context.startActivity(intent)
            }

            if (page.imageUrl == null) {
                visibility = View.GONE
            }
            addView(this)
        }

    }
}
