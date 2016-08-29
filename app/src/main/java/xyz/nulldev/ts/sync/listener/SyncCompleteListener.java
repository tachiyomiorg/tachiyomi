package xyz.nulldev.ts.sync.listener;

import xyz.nulldev.ts.sync.SyncResult;

/**
 * Project: TachiServer
 * Author: nulldev
 * Creation Date: 27/08/16
 *
 * The listener called when a sync completes
 */
public interface SyncCompleteListener {
    /**
     * The sync has completed
     * @param result The result of the sync
     */
    void onSyncComplete(SyncResult result);
}
