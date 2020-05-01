package eu.kanade.tachiyomi.ui.browse.source.browse

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
import kotlinx.android.synthetic.main.source_filter_sheet.view.filter_btn
import kotlinx.android.synthetic.main.source_filter_sheet.view.reset_btn

class SourceFilterSheet(
    activity: Activity,
    onFilterClicked: () -> Unit,
    onResetClicked: () -> Unit
) : BottomSheetDialog(activity) {

    private var filterNavView: FilterNavigationView

    init {
        filterNavView = FilterNavigationView(activity)
        filterNavView.onFilterClicked = {
            onFilterClicked()
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

        var onFilterClicked = {}
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
            filter_btn.setOnClickListener { onFilterClicked() }
            reset_btn.setOnClickListener { onResetClicked() }
        }
    }
}
