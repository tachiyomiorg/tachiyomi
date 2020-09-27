package eu.kanade.tachiyomi.widget

import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Router
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.databinding.CommonTabbedSheetBinding

abstract class TabbedBottomSheetDialog(private val router: Router) : BottomSheetDialog(router.activity!!) {

    val binding: CommonTabbedSheetBinding = CommonTabbedSheetBinding.inflate(router.activity!!.layoutInflater)

    init {
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
            return router.activity!!.resources!!.getString(getTabTitles()[position])
        }
    }
}
