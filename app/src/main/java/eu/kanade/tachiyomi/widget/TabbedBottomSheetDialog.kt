package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.CommonTabbedSheetBinding
import eu.kanade.tachiyomi.ui.manga.chapter.SetChapterSettingsDialog
import eu.kanade.tachiyomi.util.view.popupMenu

abstract class TabbedBottomSheetDialog(private val activity: Activity, private val manga: Manga? = null) : BottomSheetDialog(activity) {
    val binding: CommonTabbedSheetBinding = CommonTabbedSheetBinding.inflate(activity.layoutInflater)

    init {
        val adapter = LibrarySettingsSheetAdapter()
        binding.pager.offscreenPageLimit = 2
        binding.pager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.pager)

        binding.menu.setOnClickListener { it.post { showPopupMenu(it) } }
        setContentView(binding.root)
    }

    private fun showPopupMenu(view: View) {
        view.popupMenu(
            R.menu.default_chapter_filter,
            {
            },
            {
                when (this.itemId) {
                    R.id.save_as_default -> {
                        manga?.let { SetChapterSettingsDialog(context, it).showDialog() }
                        true
                    }
                    else -> true
                }
            }
        )
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
