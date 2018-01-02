package eu.kanade.tachiyomi.ui.sync.account

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import com.dd.processbutton.iml.ActionProcessButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
import eu.kanade.tachiyomi.data.sync.account.SyncAccountAuthenticator
import eu.kanade.tachiyomi.data.sync.protocol.snapshot.SnapshotHelper
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
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
    private val db: DatabaseHelper by injectLazy()
    private val syncManager: LibrarySyncManager by injectLazy()
    private val snapshots by lazy { SnapshotHelper(applicationContext) }
    
    var loginSubscription: Subscription? = null
    
    companion object {
        val ARG_IS_ADDING_NEW_ACCOUNT = "new_acc"
    }
    
    override fun onCreate(savedState: Bundle?) {
        //TODO Set theme
        super.onCreate(savedState)
        
        authenticatorOnCreate(savedState)
        
        setContentView(R.layout.activity_sync_auth)
    
        setSupportActionBar(toolbar)
        
        login.setMode(ActionProcessButton.Mode.ENDLESS)
        login.setOnClickListener { tryLogin() }
    }
    
    fun tryLogin() {
        login.progress = 1
        
        val url = server_input.text.toString()
        val password = password_input.text.toString()
        
        fun error(error: Int) {
            login.progress = -1
            login.setText(error)
        }
        
        try {
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
    
    fun finishLogin(url: String, password: String, token: String) {
        presenter.completeLogin(AccountManager.get(this),
                url,
                password,
                token,
                intent.getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, true))
        
        val res = Intent().putExtra(AccountManager.KEY_ACCOUNT_NAME, url)
                .putExtra(AccountManager.KEY_ACCOUNT_TYPE, SyncAccountAuthenticator.ACCOUNT_TYPE)
                .putExtra(AccountManager.KEY_AUTHTOKEN, token)
        
        //Clear snapshots
        db.deleteMangaCategoriesSnapshot(LibrarySyncManager.TARGET_DEVICE_ID).executeAsBlocking()
        db.takeEmptyMangaCategoriesSnapshot(LibrarySyncManager.TARGET_DEVICE_ID).executeAsBlocking()
        snapshots.deleteSnapshots(LibrarySyncManager.TARGET_DEVICE_ID)
        
        //Regen device ID and start sync from beginning
        syncManager.regenDeviceId()
        syncManager.lastSyncDateTime = 0
        
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
        mAccountAuthenticatorResponse = intent.getParcelableExtra<AccountAuthenticatorResponse>(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
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