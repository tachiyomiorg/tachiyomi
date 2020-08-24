package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.CommonTabbedSheetBinding
import eu.kanade.tachiyomi.util.chapter.ChapterSettingsHelper
import eu.kanade.tachiyomi.util.system.toast
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

    private lateinit var popupMenu: PopupMenu

    private fun showPopupMenu(view: View) {
        popupMenu = view.popupMenu(
            R.menu.default_chapter_filter,
            {
            },
            {
                when (this.itemId) {
                    R.id.apply_to_library -> {
                        /**
                         * keep menu open even after toggling checkbox
                         * ref: https://stackoverflow.com/a/31727213/2445763
                         */
                        this.isChecked = !this.isChecked
                        this.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                        this.actionView = View(context)
                        false
                    }
                    R.id.save_as_default -> {
                        ChapterSettingsHelper.setNewSettingDefaults(manga)
                        if (popupMenu.menu.findItem(R.id.apply_to_library).isChecked) {
                            ChapterSettingsHelper.updateAllMangasWithDefaultsFromPreferences()
                        }
                        context.toast(context.getString(R.string.chapter_settings_updated))
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
