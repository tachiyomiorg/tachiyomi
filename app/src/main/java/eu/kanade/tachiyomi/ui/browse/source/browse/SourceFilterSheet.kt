package eu.kanade.tachiyomi.ui.browse.source.browse

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.SourceFilterSheetBinding
import eu.kanade.tachiyomi.widget.SimpleNavigationView
import eu.kanade.tachiyomi.widget.sheet.BaseBottomSheetDialog

class SourceFilterSheet(
    activity: Activity,
    private val onFilterClicked: () -> Unit,
    private val onResetClicked: () -> Unit
) : BaseBottomSheetDialog(activity) {

    private var filterNavView: FilterNavigationView = FilterNavigationView(activity)

    override fun createView(inflater: LayoutInflater): View {
        filterNavView.onFilterClicked = {
            onFilterClicked()
            this.dismiss()
        }
        filterNavView.onResetClicked = onResetClicked

        return filterNavView
    }

    fun setFilters(items: List<IFlexible<*>>) {
        filterNavView.adapter.updateDataSet(items)
    }

    class FilterNavigationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) :
        SimpleNavigationView(context, attrs) {

        var onFilterClicked = {}
        var onResetClicked = {}

        val adapter: FlexibleAdapter<IFlexible<*>> = FlexibleAdapter<IFlexible<*>>(null)
            .setDisplayHeadersAtStartUp(true)

        private val binding = SourceFilterSheetBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

        init {
            recycler.adapter = adapter
            recycler.setHasFixedSize(true)
            (binding.root.getChildAt(1) as ViewGroup).addView(recycler)
            addView(binding.root)
            binding.filterBtn.setOnClickListener { onFilterClicked() }
            binding.resetBtn.setOnClickListener { onResetClicked() }
        }
    }
}
