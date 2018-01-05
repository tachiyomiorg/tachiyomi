package eu.kanade.tachiyomi.ui.sync.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
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
    private val db: DatabaseHelper by injectLazy()
    private val syncManager: LibrarySyncManager by injectLazy()
    private val network: NetworkHelper by injectLazy()
    
    fun checkLogin(server: String, password: String): Observable<AuthResponse> {
        return TWApi.create(network.client, server)
                .checkAuth(password)
    }
    
    fun completeLogin(accountManager: AccountManager,
                      url: String,
                      password: String,
                      token: String,
                      createNewAccount: Boolean) {
        val account = Account(url, LibrarySyncManager.ACCOUNT_TYPE)
        if (createNewAccount) {
            //Add account
            accountManager.addAccountExplicitly(account, password, null)
            accountManager.setAuthToken(account, SyncAccountAuthenticator.AUTH_TOKEN_TYPE, token)
    
            //Clear snapshots
            db.inTransaction {
                db.deleteMangaCategoriesSnapshot(LibrarySyncManager.TARGET_DEVICE_ID).executeAsBlocking()
                db.takeEmptyMangaCategoriesSnapshot(LibrarySyncManager.TARGET_DEVICE_ID).executeAsBlocking()
                syncManager.snapshots.deleteSnapshots(LibrarySyncManager.TARGET_DEVICE_ID)
            }
    
            //Regen device ID and start sync from beginning
            syncManager.regenDeviceId()
            syncManager.lastSyncDateTime = 0
    
            //Begin syncing automatically
            ContentResolver.setSyncAutomatically(account, LibrarySyncManager.AUTHORITY, true)
        } else {
            accountManager.setPassword(account, password)
        }
    }
}