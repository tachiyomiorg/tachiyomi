package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.chapter.ChapterSettingsHelper
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.DialogCheckboxView

class SetChapterSettingsDialog(val context: Context, val manga: Manga) {
    private var dialog: Dialog

    init {
        this.dialog = buildDialog()
    }

    private fun buildDialog(): Dialog {
        val view = DialogCheckboxView(context).apply {
            setDescription(R.string.confirm_set_chapter_settings)
            setOptionDescription(R.string.also_set_chapter_settings_for_library)
        }

        return MaterialDialog(context)
            .title(R.string.action_chapter_settings)
            .customView(
                view = view,
                horizontalPadding = true
            )
            .positiveButton(android.R.string.ok) {
                ChapterSettingsHelper.setNewSettingDefaults(manga)
                if (view.isChecked()) {
                    ChapterSettingsHelper.updateAllMangasWithDefaultsFromPreferences()
                }

                context.toast(context.getString(R.string.chapter_settings_updated))
            }
            .negativeButton(android.R.string.cancel)
    }

    fun showDialog() = dialog.show()

    fun dismissDialog() = dialog.dismiss()
}
