package eu.kanade.tachiyomi.ui.sync.account

import android.accounts.Account
import android.accounts.AccountManager
import eu.kanade.tachiyomi.data.sync.account.SyncAccountAuthenticator
import eu.kanade.tachiyomi.data.sync.api.TWApi
import eu.kanade.tachiyomi.data.sync.api.models.AuthResponse
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of [SyncAccountAuthenticatorActivity].
 */
class SyncAccountAuthenticatorPresenter : BasePresenter<SyncAccountAuthenticatorActivity>() {
    
    val network: NetworkHelper by injectLazy()
    
    fun checkLogin(server: String, password: String): Observable<AuthResponse> {
        return TWApi.create(network.client, server)
                .checkAuth(password)
    }
    
    fun completeLogin(accountManager: AccountManager,
                      url: String,
                      password: String,
                      token: String,
                      createNewAccount: Boolean) {
        val account = Account(url, SyncAccountAuthenticator.ACCOUNT_TYPE)
        if (createNewAccount) {
            accountManager.addAccountExplicitly(account, password, null)
            accountManager.setAuthToken(account, SyncAccountAuthenticator.AUTH_TOKEN_TYPE, token)
        } else {
            accountManager.setPassword(account, password)
        }
    }
}