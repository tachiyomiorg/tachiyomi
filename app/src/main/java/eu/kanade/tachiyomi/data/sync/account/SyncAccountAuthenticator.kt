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
import eu.kanade.tachiyomi.ui.sync.account.SyncAccountAuthenticatorActivity
import eu.kanade.tachiyomi.util.accountManager

/**
 * Interfaces with Android system to manage the sync accounts.
 */

class SyncAccountAuthenticator(val context: Context) : AbstractAccountAuthenticator(context) {
    companion object {
        val AUTH_TOKEN_TYPE = "full"
    }
    
    override fun getAuthTokenLabel(authTokenType: String) = null
    
    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account, options: Bundle?)
        = throw UnsupportedOperationException("Unused method")
    
    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account, authTokenType: String?, options: Bundle?)
        = throw UnsupportedOperationException("Unused method")
    
    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle?): Bundle {
        //Try to get auth token
        var authToken = context.accountManager.peekAuthToken(account, authTokenType)
        
        //No auth token!
        if(TextUtils.isEmpty(authToken)) {
            //Get auth token from server
            val apiResponse = apiFromAccount(account)
                    .checkAuth(context.accountManager.getPassword(account))
                    .toBlocking()
                    .first()
            if(apiResponse.success) {
                authToken = apiResponse.token
            }
        }
        
        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            return Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
                putString(AccountManager.KEY_AUTHTOKEN, authToken)
            }
        }
    
        //TODO Ask for authentication again
        val intent = Intent(context, SyncAccountAuthenticatorActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            putExtra(SyncAccountAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, false)
        }
        return Bundle().apply {
            putParcelable(AccountManager.KEY_INTENT, intent)
        }
    }
    
    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account, features: Array<out String>)
        = Bundle().apply {
            putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        }
    
    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String)
        = throw UnsupportedOperationException("Unused method")
    
    override fun addAccount(response: AccountAuthenticatorResponse, accountType: String, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle {
        val intent = Intent(context, SyncAccountAuthenticatorActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            putExtra(SyncAccountAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true)
        }
        return Bundle().apply {
            putParcelable(AccountManager.KEY_INTENT, intent)
        }
    }
}
