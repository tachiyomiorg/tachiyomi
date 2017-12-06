package eu.kanade.tachiyomi.data.sync.account

import android.app.Service
import android.content.Intent

/**
 * Sync auth service.
 */

class SyncAuthenticatorService: Service() {
    override fun onBind(intent: Intent) = SyncAccountAuthenticator(this).iBinder
}