package eu.kanade.tachiyomi.ui.setting.track

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.preference.LoginDialogPreference
import kotlinx.android.synthetic.main.pref_account_login.view.login
import kotlinx.android.synthetic.main.pref_account_login.view.password
import kotlinx.android.synthetic.main.pref_account_login.view.username
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackLoginDialog(
    @StringRes titleRes: Int? = null,
    titleFormatArgs: Any? = null,
    @StringRes usernameLabelRes: Int? = null,
    bundle: Bundle? = null
) : LoginDialogPreference(titleRes, titleFormatArgs, usernameLabelRes, bundle) {

    private val service = Injekt.get<TrackManager>().getService(args.getInt("key"))!!

    constructor(service: TrackService) : this(service, null)

    constructor(service: TrackService, @StringRes usernameLabelRes: Int?) :
        this(R.string.login_title, service.name, usernameLabelRes, Bundle().apply { putInt("key", service.id) })

    override fun setCredentialsOnView(view: View) = with(view) {
        username.setText(service.getUsername())
        password.setText(service.getPassword())
    }

    override fun checkLogin() {
        requestSubscription?.unsubscribe()

        v?.apply {
            if (username.text.isNullOrEmpty() || password.text.isNullOrEmpty()) {
                return
            }

            login.progress = 1
            val user = username.text.toString()
            val pass = password.text.toString()

            requestSubscription = service.login(user, pass)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        dialog?.dismiss()
                        context.toast(R.string.login_success)
                    },
                    { error ->
                        login.progress = -1
                        login.setText(R.string.unknown_error)
                        error.message?.let { context.toast(it) }
                    }
                )
        }
    }

    override fun onDialogClosed() {
        super.onDialogClosed()
        (targetController as? Listener)?.trackLoginDialogClosed(service)
    }

    interface Listener {
        fun trackLoginDialogClosed(service: TrackService)
    }
}
