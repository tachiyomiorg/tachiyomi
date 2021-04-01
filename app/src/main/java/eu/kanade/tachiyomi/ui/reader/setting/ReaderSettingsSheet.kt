package eu.kanade.tachiyomi.ui.reader.setting

import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.widget.SimpleTabSelectedListener
import eu.kanade.tachiyomi.widget.sheet.TabbedBottomSheetDialog

class ReaderSettingsSheet(private val activity: ReaderActivity) : TabbedBottomSheetDialog(activity) {

    private val readingModeSettings = ReaderReadingModeSettings(activity)
    private val generalSettings = ReaderGeneralSettings(activity)
    private val colorFilterSettings = ReaderColorFilterSettings(activity)

    private val sheetBackgroundDim = window?.attributes?.dimAmount ?: 0.25f

    init {
        val sheetBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)
        sheetBehavior.isFitToContents = false
        sheetBehavior.halfExpandedRatio = 0.5f

        val filterTabIndex = getTabViews().indexOf(colorFilterSettings)
        binding.tabs.addOnTabSelectedListener(object : SimpleTabSelectedListener() {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isFilterTab = tab?.position == filterTabIndex

                // Remove dimmed backdrop so color filter changes can be previewed
                window?.setDimAmount(if (isFilterTab) 0f else sheetBackgroundDim)

                // Hide toolbars
                if (activity.menuVisible != !isFilterTab) {
                    activity.setMenuVisibility(!isFilterTab)
                }

                // Partially collapse the sheet for better preview
                if (isFilterTab) {
                    sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
            }
        })
    }

    override fun getTabViews() = listOf(
        readingModeSettings,
        generalSettings,
        colorFilterSettings,
    )

    override fun getTabTitles() = listOf(
        R.string.pref_category_reading_mode,
        R.string.pref_category_general,
        R.string.custom_filter,
    )
}
