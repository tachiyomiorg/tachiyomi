package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.CommonTabbedSheetBinding
import eu.kanade.tachiyomi.util.chapter.ChapterSettingsHelper

abstract class TabbedBottomSheetDialog(private val activity: Activity) : BottomSheetDialog(activity) {
    val binding: CommonTabbedSheetBinding = CommonTabbedSheetBinding.inflate(activity.layoutInflater)

    init {
        val adapter = LibrarySettingsSheetAdapter()
        binding.pager.offscreenPageLimit = 2
        binding.pager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.pager)

        binding.saveChapterSettingsBtn.setOnLongClickListener {
            binding.applyChapterSettingsToAllCheckBox.visibility = View.VISIBLE
            true
        }

        setContentView(binding.root)
    }

    abstract fun getTabViews(): List<View>

    abstract fun getTabTitles(): List<Int>

    fun toggleSaveChapterSettingsBtnVisibility(m: Manga) {
        if (ChapterSettingsHelper.matchesSettingsDefaultsFromPreferences(m)) {
            binding.saveChapterSettingsBtn.visibility = View.GONE
            binding.applyChapterSettingsToAllCheckBox.visibility = View.GONE
        } else binding.saveChapterSettingsBtn.visibility = View.VISIBLE
    }

    fun setSaveChapterSettingsBtnOnClickListener(callback: () -> Unit) {
        binding.saveChapterSettingsBtn.setOnClickListener {
            callback()
            if (binding.applyChapterSettingsToAllCheckBox.isChecked) {
                ChapterSettingsHelper.updateAllMangasWithDefaultsFromPreferences()
            }
            it.visibility = View.GONE
            binding.applyChapterSettingsToAllCheckBox.visibility = View.GONE
        }
    }

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
