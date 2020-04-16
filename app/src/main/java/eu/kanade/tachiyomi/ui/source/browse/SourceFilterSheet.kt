package eu.kanade.tachiyomi.ui.source.browse

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.widget.SimpleNavigationView
import kotlinx.android.synthetic.main.source_filter_sheet.view.reset_btn
import kotlinx.android.synthetic.main.source_filter_sheet.view.search_btn

class SourceFilterSheet(
    activity: Activity,
    onSearchClicked: () -> Unit,
    onResetClicked: () -> Unit
) : BottomSheetDialog(activity) {

    private var filterNavView: FilterNavigationView

    init {
        filterNavView = FilterNavigationView(activity)
        filterNavView.onSearchClicked = {
            onSearchClicked()
            this.dismiss()
        }
        filterNavView.onResetClicked = onResetClicked

        setContentView(filterNavView)
    }

    fun setFilters(items: List<IFlexible<*>>) {
        filterNavView.adapter.updateDataSet(items)
    }

    class FilterNavigationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        SimpleNavigationView(context, attrs) {

        var onSearchClicked = {}
        var onResetClicked = {}

        val adapter: FlexibleAdapter<IFlexible<*>> = FlexibleAdapter<IFlexible<*>>(null)
            .setDisplayHeadersAtStartUp(true)
            .setStickyHeaders(true)

        init {
            recycler.adapter = adapter
            recycler.setHasFixedSize(true)
            val view = inflate(R.layout.source_filter_sheet)
            ((view as ViewGroup).getChildAt(1) as ViewGroup).addView(recycler)
            addView(view)
            search_btn.setOnClickListener { onSearchClicked() }
            reset_btn.setOnClickListener { onResetClicked() }
        }
    }
}
