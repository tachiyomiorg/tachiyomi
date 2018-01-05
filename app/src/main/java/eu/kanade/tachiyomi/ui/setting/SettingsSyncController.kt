package eu.kanade.tachiyomi.ui.setting

import android.accounts.Account
import android.accounts.OnAccountsUpdateListener
import android.os.Build
import android.support.v7.preference.PreferenceScreen
import android.support.v7.preference.SwitchPreferenceCompat
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
import eu.kanade.tachiyomi.data.sync.account.SyncAccountAuthenticator
import eu.kanade.tachiyomi.util.accountManager
import uy.kohesive.injekt.injectLazy

/**
 * Sync settings
 */
class SettingsSyncController : SettingsController(),
        OnAccountsUpdateListener {
    private val syncManager: LibrarySyncManager by injectLazy()
    
    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_sync
        
        switchPreference {
            key = PREFERENCE_SYNC_ENABLED_KEY
            titleRes = R.string.sync
            persistent = false
            
            onClick {
                activity?.let { activity ->
                    if (isChecked) {
                        activity.accountManager.addAccount(LibrarySyncManager.ACCOUNT_TYPE,
                                SyncAccountAuthenticator.AUTH_TOKEN_TYPE,
                                null,
                                null,
                                activity,
                                null,
                                null)
                    } else {
                        syncManager.account?.let { account ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                activity.accountManager.removeAccount(account, activity, null, null)
                            } else {
                                activity.accountManager.removeAccount(account, null, null)
                            }
                        }
                    }
                    
                    //Prevent switch change
                    isChecked = !isChecked
                }
            }
        }
        
        preferenceCategory {
            titleRes = R.string.pref_sync_triggers
            dependency = PREFERENCE_SYNC_ENABLED_KEY
            
            switchPreference {
                key = PreferenceKeys.syncOnLaunch
                titleRes = R.string.pref_sync_on_launch
                summaryRes = R.string.pref_sync_on_launch_summ
                defaultValue = true
            }
            
            intListPreference {
                key = PreferenceKeys.syncInterval
                titleRes = R.string.pref_sync_interval
                entriesRes = arrayOf(R.string.update_never, R.string.update_1hour,
                        R.string.update_2hour, R.string.update_3hour, R.string.update_6hour,
                        R.string.update_12hour, R.string.update_24hour, R.string.update_48hour)
                entryValues = arrayOf("0", "1", "2", "3", "6", "12", "24", "48")
                defaultValue = "0"
                summary = "%s"
        
                onChange {
                    syncManager.updatePeriodicSync((it as String).toInt())
                    true
                }
            }
        }
    
        startAccountsListener()
    }
    
    private fun startAccountsListener() {
        stopAccountsListener()
        //Begin listening for account changes (for sync button)
        //Note that we cannot use the 4-arg method as it requires a high SDK level
        activity?.accountManager?.addOnAccountsUpdatedListener(this,
                null, //Run listener on main thread
                true) //Run listener immediately with current accounts
    }
    
    private fun stopAccountsListener() {
        //Remove accounts listener
        try {
            activity?.accountManager?.removeOnAccountsUpdatedListener(this)
        } catch(ignored: IllegalStateException) {
            //Thrown if listener was never added (but doesn't matter)
        }
    }
    
    override fun onAccountsUpdated(accounts: Array<out Account>) {
        //Update sync toggle preference when accounts change
        (findPreference(PREFERENCE_SYNC_ENABLED_KEY) as? SwitchPreferenceCompat)?.apply {
            val account = syncManager.account
            val syncEnabled = account != null
            
            isChecked = syncEnabled
            
            //Set summary
            summary = if(syncEnabled) {
                activity?.getString(R.string.pref_sync_details_prefix, account!!.name)
                        ?: return@apply
            } else {
                ""
            }
        }
    }
    
    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        
        stopAccountsListener()
    }
    
    companion object {
        //Nonpersistent preference keys
        private val PREFERENCE_SYNC_ENABLED_KEY = "pref_sync_enabled"
    }
}