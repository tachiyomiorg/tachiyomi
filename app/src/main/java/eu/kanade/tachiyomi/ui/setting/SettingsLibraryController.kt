package eu.kanade.tachiyomi.ui.setting


import android.app.Dialog
import android.os.Bundle
import android.support.v7.preference.PreferenceScreen
import android.widget.EditText
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.local_library_dialog.view.*
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

private var localdir:EditText? = null

class SettingsLibraryController: SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_library

        preference {
            key = PreferenceKeys.localDirectory
            titleRes = R.string.pref_local_directory
            onClick {
                val ctrl = LocalDirectoriesDialog()
                ctrl.targetController = this@SettingsLibraryController
                ctrl.showDialog(router)
            }
            preferences.localDirectory().asObservable()
                    .subscribeUntilDestroy { path ->
                        summary = path
                    }
        }
        switchPreference {
            key = Keys.enableAltStorage
            titleRes = R.string.pref_enable_alt_storage
            defaultValue = false
        }
    }

    class LocalDirectoriesDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val dialog = MaterialDialog.Builder(activity!!)
                    .title("Custom Path")
                    .customView(R.layout.local_library_dialog,false)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive { _, _ -> onPositiveButtonClick() }
                    .build()

          localdir = dialog.customView?.local_library_path
            localdir?.setText(  preferences.localDirectory().get())
            return dialog
        }

        private fun onPositiveButtonClick() {
            preferences.localDirectory().set(localdir?.text.toString())

        }
}
}