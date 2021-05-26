package eu.kanade.tachiyomi.ui.reader.setting

import android.animation.ValueAnimator
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.widget.listener.SimpleTabSelectedListener
import eu.kanade.tachiyomi.widget.sheet.TabbedBottomSheetDialog

class ReaderSettingsSheet(
    private val activity: ReaderActivity,
    private val showColorFilterSettings: Boolean = false,
) : TabbedBottomSheetDialog(activity) {

    private val readingModeSettings = ReaderReadingModeSettings(activity)
    private val generalSettings = ReaderGeneralSettings(activity)
    private val colorFilterSettings = ReaderColorFilterSettings(activity)

    private val backgroundDimAnimator by lazy {
        val sheetBackgroundDim = window?.attributes?.dimAmount ?: 0.25f
        ValueAnimator.ofFloat(sheetBackgroundDim, 0f).also { valueAnimator ->
            valueAnimator.duration = 250
            valueAnimator.addUpdateListener {
                window?.setDimAmount(it.animatedValue as Float)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        behavior.isFitToContents = false
        behavior.halfExpandedRatio = 0.25f

        val filterTabIndex = getTabViews().indexOf(colorFilterSettings)
        binding.tabs.addOnTabSelectedListener(object : SimpleTabSelectedListener() {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isFilterTab = tab?.position == filterTabIndex

                // Remove dimmed backdrop so color filter changes can be previewed
                backgroundDimAnimator.run {
                    if (isFilterTab) {
                        if (animatedFraction < 1f) {
                            start()
                        }
                    } else if (animatedFraction > 0f) {
                        reverse()
                    }
                }

                // Hide toolbars
                if (activity.menuVisible != !isFilterTab) {
                    activity.setMenuVisibility(!isFilterTab)
                }
            }
        })

        if (showColorFilterSettings) {
            binding.tabs.getTabAt(filterTabIndex)?.select()
        }
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
