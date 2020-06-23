package eu.kanade.tachiyomi.ui.reader

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.DeepLinkActivity
import eu.kanade.tachiyomi.ui.main.MainActivity.Companion.INTENT_SEARCH
import eu.kanade.tachiyomi.ui.main.MainActivity.Companion.INTENT_SEARCH_QUERY
import kotlin.math.max
import kotlin.math.min
import kotlinx.android.synthetic.main.reader_extract_page_text_sheet.*
import timber.log.Timber

/**
 * Sheet to show when a page is long clicked.
 */
class ReaderExtractedTextSheet(
    private val activity: ReaderActivity,
    private val text: String
) : BottomSheetDialog(activity) {

    private val view = activity.layoutInflater.inflate(R.layout.reader_extract_page_text_sheet, null)
    private val searchTerms = mutableSetOf<String>()

    init {
        setContentView(view)
        extracted_page_text.text = text

        extracted_page_text.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            val QUEUE_SEARCH_TERM = 999

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                try {
                    val field = menu!!.javaClass.getDeclaredField("mOptionalIconsVisible")
                    field.isAccessible = true
                    field.setBoolean(menu, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.add(0, QUEUE_SEARCH_TERM, 0, "Queue")?.setIcon(R.drawable.ic_explore_outline_24dp)
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) { }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    QUEUE_SEARCH_TERM -> {
                        // ref: https://stackoverflow.com/questions/22832123/get-selected-text-from-textview
                        var min = 0
                        var max: Int = extracted_page_text.text.length

                        if (extracted_page_text.isFocused) {
                            val selStart: Int = extracted_page_text.selectionStart
                            val selEnd: Int = extracted_page_text.selectionEnd
                            min = max(0, min(selStart, selEnd))
                            max = max(0, max(selStart, selEnd))
                        }

                        // Perform your definition lookup with the selected text
                        val selectedText = extracted_page_text.text.subSequence(min, max).toString()
                        mode?.finish()

                        if (selectedText.isNotBlank()) {
                            searchTerms.add(selectedText)
                            updateSearchButton()
                        }

                        return true
                    }
                }

                return false
            }
        }

        extracted_search_button.setOnClickListener {
            val searchQuery = searchTerms.joinToString(
                separator = " ",
                transform = { "\"$it\"" }
            )

            Timber.d("searchQuery: $searchQuery")

            val intent = Intent().apply {
                setClass(activity.applicationContext, DeepLinkActivity::class.java)
                action = INTENT_SEARCH
                putExtra(INTENT_SEARCH_QUERY, searchQuery)
            }

            dismiss()
            activity.startActivity(intent)
        }

        setOnDismissListener {
            searchTerms.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
        if (width > 0) {
            window?.setLayout(width, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    private fun updateSearchButton() {
        when {
            searchTerms.size < 1 -> {
                extracted_search_button.visibility = View.GONE
            }
            else -> {
                extracted_search_button.text = activity.getString(R.string.extract_text_search_terms, searchTerms.size)
                extracted_search_button.visibility = View.VISIBLE
            }
        }
    }
}
