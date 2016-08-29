package eu.kanade.tachiyomi.ui.library.sync

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager

class LibrarySyncDialogFragment : DialogFragment() {
    companion object {
        fun show(fragmentManager: FragmentManager) {
            LibrarySyncDialogFragment().show(fragmentManager, "library_sync")
        }
    }

    val librarySyncDialog by lazy {
        LibrarySyncDialog(activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        //Create new dialog, call this before runSync to ensure runSync has a dialog to work with
        val dialog = librarySyncDialog.createDialog()
        //Run sync if it has not yet
        if (savedState == null) {
            librarySyncDialog.runSync()
        }
        return dialog
    }

    //http://stackoverflow.com/questions/14657490/how-to-properly-retain-a-dialogfragment-through-rotation
    override fun onDestroyView() {
        val dialog = dialog
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }
        super.onDestroyView()
    }
}