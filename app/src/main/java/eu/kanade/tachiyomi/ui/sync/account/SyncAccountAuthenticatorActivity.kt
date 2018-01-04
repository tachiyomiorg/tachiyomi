package eu.kanade.tachiyomi.ui.sync.account

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.dd.processbutton.iml.ActionProcessButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.util.accountManager
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.activity_sync_auth.*
import nucleus.factory.RequiresPresenter
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

/**
 * Sync authentication UI.
 */

@RequiresPresenter(SyncAccountAuthenticatorPresenter::class)
class SyncAccountAuthenticatorActivity : BaseRxActivity<SyncAccountAuthenticatorPresenter>() {
    private val syncManager: LibrarySyncManager by injectLazy()
    
    private var loginSubscription: Subscription? = null
    
    companion object {
        val ARG_IS_ADDING_NEW_ACCOUNT = "new_acc"
    }
    
    private val isAddingNewAccount
        get() = intent.getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, true)
    
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        
        authenticatorOnCreate(savedState)
        
        setContentView(R.layout.activity_sync_auth)
    
        setSupportActionBar(toolbar)
        
        login.setMode(ActionProcessButton.Mode.ENDLESS)
        login.setOnClickListener { tryLogin() }
        
        //Fill in username if using old account
        if(!isAddingNewAccount) {
            server_input.setText(syncManager.account?.name ?: run {
                //No old account???
                toast(R.string.sync_error_no_account)
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
                ""
            })
            server_input.isEnabled = false
        }
        
        //Max one account
        if(isAddingNewAccount && syncManager.account != null) {
            toast(R.string.sync_error_account_exists)
            setResult(Activity.RESULT_CANCELED, Intent())
            finish()
        }
    }
    
    fun tryLogin() {
        fun error(error: Int) {
            login.progress = -1
            login.setText(error)
        }
    
        //Max one account (again as account could have been added while activity is open)
        if(isAddingNewAccount && syncManager.account != null) {
            error(R.string.sync_error_account_exists_short)
        }
        
        login.progress = 1
        
        val url = server_input.text.toString().let {
            if(!it.endsWith('/'))
                "$it/"
            else it
        }
        val password = password_input.text.toString()
        
        try {
            loginSubscription?.unsubscribe()
            loginSubscription = presenter.checkLogin(url, password)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it.success) {
                            toast(R.string.login_success)
                            finishLogin(url, password, it.token)
                        } else {
                            error(R.string.invalid_login)
                        }
                    }, {
                        error(R.string.unknown_error)
                        Timber.e(it, "An exception was thrown while logging into sync!")
                    })
        } catch(e: Exception) {
            error(R.string.unknown_error)
            Timber.e(e, "An exception was thrown while logging into sync!")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loginSubscription?.unsubscribe()
    }
    
    fun finishLogin(url: String, password: String, token: String) {
        presenter.completeLogin(accountManager,
                url,
                password,
                token,
                isAddingNewAccount)
        
        val res = Intent().putExtra(AccountManager.KEY_ACCOUNT_NAME, url)
                .putExtra(AccountManager.KEY_ACCOUNT_TYPE, LibrarySyncManager.ACCOUNT_TYPE)
                .putExtra(AccountManager.KEY_AUTHTOKEN, token)
    
        setAccountAuthenticatorResult(res.extras)
        setResult(RESULT_OK, res)
        finish()
    }
    
    /** All the code below is boilerplate code from [android.accounts.AccountAuthenticatorActivity] **/
    private var mAccountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null
    
    fun setAccountAuthenticatorResult(result: Bundle) {
        mResultBundle = result
    }
    
    fun authenticatorOnCreate(savedState: Bundle?) {
        mAccountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        mAccountAuthenticatorResponse?.onRequestContinued()
    }
    
    override fun finish() {
        if (mAccountAuthenticatorResponse != null) {
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse!!.onResult(mResultBundle)
            } else {
                mAccountAuthenticatorResponse!!.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled")
            }
            mAccountAuthenticatorResponse = null
        }
        super.finish()
    }
}