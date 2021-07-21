package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.dd.processbutton.iml.ActionProcessButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PrefAccountLoginBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.injectLazy

abstract class LoginDialogPreference(
    @StringRes private val usernameLabelRes: Int? = null,
    bundle: Bundle? = null
) : DialogController(bundle) {

    var binding: PrefAccountLoginBinding? = null
        private set

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = PrefAccountLoginBinding.inflate(LayoutInflater.from(activity!!))
        onViewCreated(binding!!.root)
        val titleName = activity!!.getString(getTitleName())
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(activity!!.getString(R.string.login_title, titleName))
            .setView(binding!!.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
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

    @StringRes
    protected abstract fun getTitleName(): Int

    protected abstract fun checkLogin()

    protected abstract fun setCredentialsOnView(view: View)
}
