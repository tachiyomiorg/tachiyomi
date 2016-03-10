package eu.kanade.tachiyomi.widget.preference

import android.os.Bundle
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.mangasync.base.MangaSyncService
import eu.kanade.tachiyomi.ui.setting.SettingsActivity
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.pref_account_login.view.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MangaSyncLoginDialog : LoginDialogPreference() {

    companion object {

        fun newInstance(sync: MangaSyncService): LoginDialogPreference {
            val fragment = MangaSyncLoginDialog()
            val bundle = Bundle(1)
            bundle.putInt("key", sync.id)
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var sync: MangaSyncService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val syncId = arguments.getInt("key")
        sync = (activity as SettingsActivity).syncManager.getService(syncId)
    }

    override fun setCredentialsOnView(view: View) = with(view) {
        title.text = getString(R.string.login_title, sync.name)
        username.setText(preferences.getMangaSyncUsername(sync))
        password.setText(preferences.getMangaSyncPassword(sync))
    }

    override fun checkLogin() {
        requestSubscription?.unsubscribe()

        v?.apply {
            if (username.text.length == 0 || password.text.length == 0)
                return

            login.progress = 1

            requestSubscription = sync.login(username.text.toString(), password.text.toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ logged ->
                        if (logged) {
                            preferences.setMangaSyncCredentials(sync,
                                    username.text.toString(),
                                    password.text.toString())

                            dialog.dismiss()
                            context.toast(R.string.login_success)
                        } else {
                            preferences.setMangaSyncCredentials(sync, "", "")
                            login.progress = -1
                        }
                    }, { error ->
                        login.progress = -1
                        login.setText(R.string.unknown_error)
                    })

        }
    }

}
