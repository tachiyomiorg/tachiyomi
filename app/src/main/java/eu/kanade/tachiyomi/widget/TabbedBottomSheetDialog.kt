package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.databinding.CommonTabbedSheetBinding

abstract class TabbedBottomSheetDialog(private val activity: Activity) : BottomSheetDialog(activity) {

    init {
        val binding: CommonTabbedSheetBinding = CommonTabbedSheetBinding.inflate(activity.layoutInflater)

        val adapter = LibrarySettingsSheetAdapter()
        binding.pager.offscreenPageLimit = 2
        binding.pager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.pager)

        setContentView(binding.root)
    }

    abstract fun getTabViews(): List<View>

    abstract fun getTabTitles(): List<Int>

    private inner class LibrarySettingsSheetAdapter : ViewPagerAdapter() {

        override fun createView(container: ViewGroup, position: Int): View {
            return getTabViews()[position]
        }

        override fun getCount(): Int {
            return getTabViews().size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return activity.resources!!.getString(getTabTitles()[position])
        }
    }
}
