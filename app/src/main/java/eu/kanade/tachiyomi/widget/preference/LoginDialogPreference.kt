package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.dd.processbutton.iml.ActionProcessButton
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PrefAccountLoginBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.injectLazy

abstract class LoginDialogPreference(
    @StringRes private val titleRes: Int? = null,
    private val titleFormatArgs: Any? = null,
    @StringRes private val usernameLabelRes: Int? = null,
    bundle: Bundle? = null
) : DialogController(bundle) {

    var binding: PrefAccountLoginBinding? = null
        private set

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = PrefAccountLoginBinding.inflate(LayoutInflater.from(activity!!))
        var dialog = MaterialDialog(activity!!)
            .customView(view = binding!!.root)
            .negativeButton(android.R.string.cancel)

        if (titleRes != null) {
            dialog = dialog.title(text = activity!!.getString(titleRes, titleFormatArgs))
        }

        onViewCreated(dialog.view)

        return dialog
    }

    fun onViewCreated(view: View) {
        if (usernameLabelRes != null) {
            binding!!.usernameLabel.hint = view.context.getString(usernameLabelRes)
        }

        binding!!.login.setMode(ActionProcessButton.Mode.ENDLESS)
        binding!!.login.setOnClickListener { checkLogin() }

        setCredentialsOnView(view)
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (!type.isEnter) {
            onDialogClosed()
        }
    }

    open fun onDialogClosed() {
        binding = null
    }

    protected abstract fun checkLogin()

    protected abstract fun setCredentialsOnView(view: View)
}
