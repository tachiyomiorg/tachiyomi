package eu.kanade.tachiyomi.ui.main

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.util.AttributeSet
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.updater.UpdateCheckerJob
import it.gmariotti.changelibs.library.view.ChangeLogRecyclerView

class ChangelogDialogFragment : DialogFragment() {

    companion object {
        fun show(preferences: PreferencesHelper, fragmentManager: FragmentManager) {
            val oldVersion = preferences.lastVersionCode().getOrDefault()
            if (oldVersion < BuildConfig.VERSION_CODE) {
                preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)
                ChangelogDialogFragment().show(fragmentManager, "changelog")

                // FIXME Ugly check to restore jobs. Remove me in a few months :D
                if (oldVersion < 14) {
                    if (BuildConfig.INCLUDE_UPDATER && preferences.automaticUpdates()) {
                        UpdateCheckerJob.setupTask()
                    }
                    LibraryUpdateJob.setupTask()
                }
            }
        }
    }

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val view = WhatsNewRecyclerView(context)
        return MaterialDialog.Builder(activity)
                .title(if (BuildConfig.DEBUG) "Notices" else "Changelog")
                .customView(view, false)
                .positiveText(android.R.string.yes)
                .build()
    }

    class WhatsNewRecyclerView(context: Context) : ChangeLogRecyclerView(context) {
        override fun initAttrs(attrs: AttributeSet?, defStyle: Int) {
            mRowLayoutId = R.layout.changelog_row_layout
            mRowHeaderLayoutId = R.layout.changelog_header_layout
            mChangeLogFileResourceId = if (BuildConfig.DEBUG) R.raw.changelog_debug else R.raw.changelog_release
        }
    }
}