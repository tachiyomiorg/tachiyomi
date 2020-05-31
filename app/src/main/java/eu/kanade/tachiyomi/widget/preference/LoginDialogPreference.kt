package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.dd.processbutton.iml.ActionProcessButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import kotlinx.android.synthetic.main.pref_account_login.view.login
import kotlinx.android.synthetic.main.pref_account_login.view.username_label
import rx.Subscription
import uy.kohesive.injekt.injectLazy

abstract class LoginDialogPreference(
    @StringRes private val titleRes: Int? = null,
    private val titleFormatArgs: Any? = null,
    @StringRes private val usernameLabelRes: Int? = null,
    bundle: Bundle? = null
) : DialogController(bundle) {

    var v: View? = null
        private set

    val preferences: PreferencesHelper by injectLazy()

    var requestSubscription: Subscription? = null

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        var dialog = MaterialDialog(activity!!)
            .customView(R.layout.pref_account_login)
            .negativeButton(android.R.string.cancel)

        if (titleRes != null) {
            dialog = dialog.title(text = activity!!.getString(titleRes, titleFormatArgs))
        }

        onViewCreated(dialog.view)

        return dialog
    }

    fun onViewCreated(view: View) {
        v = view.apply {
            if (usernameLabelRes != null) {
                username_label.hint = context.getString(usernameLabelRes)
            }

            login.setMode(ActionProcessButton.Mode.ENDLESS)
            login.setOnClickListener { checkLogin() }

            setCredentialsOnView(this)
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (!type.isEnter) {
            onDialogClosed()
        }
    }

    open fun onDialogClosed() {
        requestSubscription?.unsubscribe()
    }

    protected abstract fun checkLogin()

    protected abstract fun setCredentialsOnView(view: View)
}
