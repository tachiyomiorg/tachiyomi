package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.librarysync.LibrarySyncManager
import eu.kanade.tachiyomi.ui.library.sync.LibrarySyncDialog
import uy.kohesive.injekt.injectLazy

class SettingsLibrarySyncFragment : SettingsFragment() {

    companion object {
        fun newInstance(rootKey: String): SettingsLibrarySyncFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsLibrarySyncFragment().apply { arguments = args }
        }
    }

    private val syncNow by lazy { findPreference(getString(R.string.pref_library_sync_now_key)) }

    private val overwriteServerLibrary by lazy { findPreference(getString(R.string.pref_library_sync_delete_last_library_state_key)) }

    private val sync: LibrarySyncManager by injectLazy()

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        syncNow.setOnPreferenceClickListener {
            val librarySyncDialog = LibrarySyncDialog(activity)
            val dialog = librarySyncDialog.createDialog()
            dialog.show()
            doKeepDialog(dialog)
            librarySyncDialog.runSync()
            true
        }

        overwriteServerLibrary.setOnPreferenceClickListener {
            sync.getAbsoluteLastLibraryFile().delete()
            Snackbar.make(view, R.string.library_sync_last_library_state_deleted, Snackbar.LENGTH_SHORT).show()
            true
        }
    }

    /**
     * Make sure dialogs not using DialogFragment do not get dismissed on
     */
    private fun doKeepDialog(dialog: Dialog) {
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window.attributes = lp
    }
}
