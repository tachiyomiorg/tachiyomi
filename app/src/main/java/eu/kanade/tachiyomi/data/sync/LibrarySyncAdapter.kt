package eu.kanade.tachiyomi.data.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.sync.account.SyncAccountAuthenticator
import eu.kanade.tachiyomi.data.sync.api.TWApi
import eu.kanade.tachiyomi.data.sync.protocol.ReportApplier
import eu.kanade.tachiyomi.data.sync.protocol.ReportGenerator
import eu.kanade.tachiyomi.network.NetworkHelper
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * Core sync code
 */

class LibrarySyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true, false) {
    
    val db: DatabaseHelper by injectLazy()
    
    val gson: Gson by injectLazy()
    
    val syncManager: LibrarySyncManager by injectLazy()
    
    val networkService: NetworkHelper by injectLazy()
    
    val accountManager: AccountManager by lazy { AccountManager.get(context) }
    
    val jsonParser : JsonParser by lazy { JsonParser() }
    
    val reportGenerator by lazy { ReportGenerator() }
    val reportApplier by lazy { ReportApplier() }
    
    //TODO Exception handling
    override fun onPerformSync(account: Account, extras: Bundle?, authority: String?, provider: ContentProviderClient?, syncResult: SyncResult?) {
        //Generate library diff
        val diff = reportGenerator.gen(0)
        
        //Upload diff
        val api = TWApi.apiFromAccount(account)
        var token: String? = null
        //Three tries to authenticate
        for(i in 1 .. 3) {
            token = accountManager.blockingGetAuthToken(account,
                    SyncAccountAuthenticator.AUTH_TOKEN_TYPE,
                    true) ?: return
            //Verify we are authenticated first
            if (api.testAuthenticated(token)
                    .toBlocking()
                    .first()
                    .success) {
                break
            } else {
                //Unsuccessful, get a new auth token
                accountManager.invalidateAuthToken(SyncAccountAuthenticator.ACCOUNT_TYPE,
                        token)
                token = null
                Timber.w("Sync authentication token is invalid, retrieving a new one!")
            }
        }
        if(token == null) {
            return //Still no valid token, die
        }
        
        //Actually upload diff
        //TODO Error handling
        val result = api.sync(token, diff).toBlocking().first()
        reportApplier.apply(result.serverChanges!!)
    }
}