package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.LocaleHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionUninstallWarnDialog : DialogController {

    val sourceManager: SourceManager = Injekt.get()

    val sources: List<Source>

    constructor(dependentSources: List<Source>) : super(
        bundleOf(
            KEY_SOURCE_IDS to dependentSources.map { it.id }.toTypedArray()
        )
    ) {

        sources = dependentSources
    }

    /**
     * Restore dialog.
     * @param bundle bundle containing data from state restore.
     */
    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        // Get list of source IDs from bundle
        val sourceIDs = bundle.getLongArray(KEY_SOURCE_IDS)

        sources = sourceIDs?.map { id ->
            sourceManager.get(id)
        }?.filterNotNull()?.toList() ?: emptyList()
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val diagMessage = activity!!.applicationContext.getString(R.string.ext_uninstall_warning_message, sources.joinToString("\n") { "- ${it.name} (${LocaleHelper.getDisplayName(it.lang)})" })
        return MaterialDialog(activity!!).show {
            message(text = diagMessage)

            @Suppress("DEPRECATION")
            neutralButton(R.string.ext_uninstall) { uninstallCallback() }
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.migrate) { migrationCallback() }
        }
    }

    var migrationCallback: () -> Unit = {}

    var uninstallCallback: () -> Unit = {}

    private companion object {
        const val KEY_SOURCE_IDS = "ExtensionUninstallWarnDialog.array.int.sourceids"
    }
}
