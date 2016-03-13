package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.dd.processbutton.iml.ActionProcessButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import eu.kanade.tachiyomi.widget.SimpleTextWatcher
import kotlinx.android.synthetic.main.pref_account_login.view.*
import rx.Subscription

abstract class LoginDialogPreference : DialogFragment() {

    var v: View? = null
        private set

    val preferences: PreferencesHelper
        get() = (activity as SettingsActivity).preferences

    var requestSubscription: Subscription? = null

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity)
                .customView(R.layout.pref_account_login, false)
                .negativeText(android.R.string.cancel)
                .build();

        onViewCreated(dialog.view, savedState)

        return dialog
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        v = view.apply {
            show_password.setOnCheckedChangeListener { v, isChecked ->
                if (isChecked)
                    password.transformationMethod = null
                else
                    password.transformationMethod = PasswordTransformationMethod()
            }

            login.setMode(ActionProcessButton.Mode.ENDLESS)
            login.setOnClickListener { checkLogin() }

            setCredentialsOnView(this)

            show_password.isEnabled = password.text.isNullOrEmpty()

            password.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.length == 0) {
                        show_password.isEnabled = true
                    }
                }
            })
        }

    }

    override fun onPause() {
        super.onPause()
        requestSubscription?.unsubscribe()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        targetFragment?.onActivityResult(targetRequestCode, arguments.getInt("key"), Intent())
    }

    protected abstract fun checkLogin()

    protected abstract fun setCredentialsOnView(view: View)

}
