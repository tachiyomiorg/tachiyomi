package eu.kanade.tachiyomi.widget.preference

import android.os.Bundle
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.pref_account_login.view.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

class SourceLoginDialog : LoginDialogPreference() {

    companion object {

        fun newInstance(source: Source): LoginDialogPreference {
            val fragment = SourceLoginDialog()
            val bundle = Bundle(1)
            bundle.putLong("key", source.id)
            fragment.arguments = bundle
            return fragment
        }
    }

    val sourceManager: SourceManager by injectLazy()

    lateinit var source: LoginSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceId = arguments.getLong("key")
        source = sourceManager.get(sourceId) as LoginSource
    }

    override fun setCredentialsOnView(view: View) = with(view) {
        dialog_title.text = getString(R.string.login_title, source.toString())
        username.setText(preferences.sourceUsername(source))
        password.setText(preferences.sourcePassword(source))
    }

    override fun checkLogin() {
        requestSubscription?.unsubscribe()

        v?.apply {
            if (username.text.isEmpty() || password.text.isEmpty())
                return

            login.progress = 1

            requestSubscription = source.login(username.text.toString(), password.text.toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ logged ->
                        if (logged) {
                            preferences.setSourceCredentials(source,
                                    username.text.toString(),
                                    password.text.toString())

                            dialog.dismiss()
                            context.toast(R.string.login_success)
                        } else {
                            preferences.setSourceCredentials(source, "", "")
                            login.progress = -1
                        }
                    }, { error ->
                        login.progress = -1
                        login.setText(R.string.unknown_error)
                        error.message?.let { context.toast(it) }
                    })
        }
    }

}
