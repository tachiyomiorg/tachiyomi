package eu.kanade.tachiyomi.data.sync.account

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import eu.kanade.tachiyomi.data.sync.api.TWApi.Companion.apiFromAccount
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.sync.account.SyncAccountAuthenticatorActivity
import uy.kohesive.injekt.injectLazy

/**
 * Interfaces with Android system to manage the sync accounts.
 */

class SyncAccountAuthenticator(val context: Context?) : AbstractAccountAuthenticator(context) {
    
    companion object {
        val ACCOUNT_TYPE = "eu.kanade.tachiyomi.data.sync"
        val AUTH_TOKEN_TYPE = "full"
    }
    
    val networkService: NetworkHelper by injectLazy()
    
    override fun getAuthTokenLabel(authTokenType: String) = null
    
    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account, options: Bundle?): Bundle {
        //TODO Implement (not required for now)
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    
    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account, authTokenType: String?, options: Bundle?): Bundle {
        //TODO Implement (not required for now)
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    
    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle?): Bundle {
        val am = AccountManager.get(context)
        
        var authToken = am.peekAuthToken(account, authTokenType)
        
        //No auth token!
        if(TextUtils.isEmpty(authToken)) {
            val apiResponse = apiFromAccount(account)
                    .checkAuth(am.getPassword(account))
                    .toBlocking()
                    .first()
            if(apiResponse.success) {
                authToken = apiResponse.token
            }
        }
        
        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }
        
        //TODO Ask for authentication again
        val intent = Intent(context, SyncAccountAuthenticatorActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(SyncAccountAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, false)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }
    
    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account, features: Array<out String>): Bundle {
        val bundle = Bundle()
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return bundle
    }
    
    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle {
        //TODO Implement (not required for now)
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    
    override fun addAccount(response: AccountAuthenticatorResponse, accountType: String, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle {
        val intent = Intent(context, SyncAccountAuthenticatorActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(SyncAccountAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }
}
