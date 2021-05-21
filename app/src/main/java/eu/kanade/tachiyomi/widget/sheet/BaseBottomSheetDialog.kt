package eu.kanade.tachiyomi.widget.sheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R

abstract class BaseBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    internal lateinit var sheetBehavior: BottomSheetBehavior<*>

    abstract fun createView(inflater: LayoutInflater): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = createView(layoutInflater)
        setContentView(rootView)

        sheetBehavior = BottomSheetBehavior.from(rootView.parent as ViewGroup)

        // Enforce max width for tablets
        val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
        if (width > 0) {
            sheetBehavior.maxWidth = width
        }
    }
}
