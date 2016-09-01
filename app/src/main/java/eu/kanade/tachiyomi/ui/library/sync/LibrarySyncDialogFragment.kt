package eu.kanade.tachiyomi.ui.library.sync

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxDialogFragment
import nucleus.factory.RequiresPresenter
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

@RequiresPresenter(LibrarySyncPresenter::class)
class LibrarySyncDialogFragment : BaseRxDialogFragment<LibrarySyncPresenter>() {
    companion object {
        fun show(fragmentManager: FragmentManager) {
            LibrarySyncDialogFragment().show(fragmentManager, "library_sync")
        }
    }

    /**
     * Subscription for sync progress
     */
    private var syncProgressSubscription: Subscription? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return showSyncDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupSubscription()
    }

    fun cleanupSubscription() {
        syncProgressSubscription?.unsubscribe()
        syncProgressSubscription = null
    }

    /**
     * Create a dialog that will display sync progress
     */
    fun showSyncDialog(): MaterialDialog {
        cleanupSubscription()
        //Listen for sync events
        syncProgressSubscription = presenter.syncProgressSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is LibrarySyncProgressEvent.Completed -> {
                            dialog?.dismiss()
                            //Report conflicts if we have any
                            if (it.conflicts.size > 0)
                                basicDialog(R.string.sync_conflicts,
                                        it.conflicts.joinToString(separator = "\n"))
                        }
                        is LibrarySyncProgressEvent.Error -> {
                            dialog?.dismiss()
                            //Show error
                            basicDialog(R.string.sync_error, it.error)
                        }
                        is LibrarySyncProgressEvent.ProgressUpdated -> (dialog as? MaterialDialog)?.setContent(it.status)
                    }
                }
        return MaterialDialog.Builder(activity)
                .progress(true, 0)
                .title(R.string.library_sync_dialog_title)
                .content(R.string.uploading_library)
                .cancelable(false)
                .build()
    }

    /**
     * Show a dialog with a String resource title, a String body and an OK button
     *
     * @param title The String resource title
     * @param content The content
     */
    private fun basicDialog(title: Int, content: String) {
        MaterialDialog.Builder(activity)
                .title(title)
                .content(content)
                .cancelable(true)
                .positiveText(android.R.string.ok)
                .show()
    }
}