package xyz.nulldev.ts.sync.listener;

/**
 * Project: TachiServer
 * Author: nulldev
 * Creation Date: 27/08/16
 *
 * The listener called when the progress of a synchronization operation changes
 */
public interface ProgressChangeListener {
    /**
     * The progress of a sync operation changed
     * @param isComplete Whether or not the sync operation has completed
     * @param details The progress details
     */
    void onProgressChange(boolean isComplete, String details);
}
