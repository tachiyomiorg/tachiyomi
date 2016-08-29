package xyz.nulldev.ts.sync;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import xyz.nulldev.ts.sync.http.DefaultHttpClient;
import xyz.nulldev.ts.sync.http.HttpClient;
import xyz.nulldev.ts.sync.listener.ProgressChangeListener;
import xyz.nulldev.ts.sync.listener.SyncCompleteListener;

import java.io.IOException;
import java.util.Collections;

/**
 * Project: TachiServer
 * Author: nulldev
 * Creation Date: 26/08/16
 * <p/>
 * An example sync client implementation
 */
public class SyncClient {

    private HttpClient httpClient = new DefaultHttpClient();
    private String syncEndpoint;
    private String taskEndpoint;
    private boolean favoritesOnly = false;
    private int progressUpdateFrequency = 250; //Interval between progress updates in ms

    public SyncClient(String syncEndpoint, String taskEndpoint) {
        this.syncEndpoint = syncEndpoint;
        this.taskEndpoint = taskEndpoint;
    }

    public SyncClient(String syncEndpoint, String taskEndpoint, boolean favoritesOnly) {
        this.syncEndpoint = syncEndpoint;
        this.taskEndpoint = taskEndpoint;
        this.favoritesOnly = favoritesOnly;
    }

    /**
     * Synchronously sync libraries
     *
     * @param oldLibrary The old library
     * @param newLibrary The new library
     * @return The sync result
     */
    public SyncResult syncLibraries(String oldLibrary, String newLibrary) {
        try {
            long taskId = trySync(oldLibrary, newLibrary);
            while (true) {
                JSONObject taskStatus = getTaskStatus(taskId);
                JSONObject taskDetails = getTaskDetails(taskStatus);
                if (isSyncComplete(taskStatus, taskDetails)) {
                    return taskDetailsToSyncResult(taskDetails);
                }
                if (quietSleep(progressUpdateFrequency)) return SyncResult.fail();
            }
        } catch (Throwable t) {
            return SyncResult.fail(t.getMessage());
        }
    }

    /**
     * Sync libraries asynchronously
     *
     * @param oldLibrary             The old library
     * @param newLibrary             The new library
     * @param syncCompleteListener   The listener to be called when the sync is completed
     * @param progressChangeListener The listener to be called when the progress of the sync changes
     */
    public void syncLibrariesWithProgress(
            final String oldLibrary,
            final String newLibrary,
            final SyncCompleteListener syncCompleteListener,
            final ProgressChangeListener progressChangeListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final long taskId = trySync(oldLibrary, newLibrary);
                    while (true) {
                        JSONObject taskStatus = SyncClient.this.getTaskStatus(taskId);
                        JSONObject taskDetails = SyncClient.this.getTaskDetails(taskStatus);
                        String statusString = taskDetails.getString("status");
                        if (SyncClient.this.isSyncComplete(taskStatus, taskDetails)) {
                            syncCompleteListener.onSyncComplete(SyncClient.this.taskDetailsToSyncResult(taskDetails));
                            progressChangeListener.onProgressChange(true, statusString);
                            return;
                        } else {
                            progressChangeListener.onProgressChange(false, statusString);
                        }
                        if (SyncClient.this.quietSleep(progressUpdateFrequency)) {
                            syncCompleteListener.onSyncComplete(SyncResult.fail());
                            return;
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    syncCompleteListener.onSyncComplete(SyncResult.fail(t.getMessage()));
                }
            }
        }).start();
    }

    /**
     * Wrapper around Thread.sleep() without that annoying InterruptedException.
     *
     * @param ms the ms to sleep
     * @return Whether or not the task was interrupted
     */
    private boolean quietSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            return true;
        }
        return false;
    }

    /**
     * Convert a task detail to a sync result (only call this when the sync is complete without errors)
     *
     * @param taskDetails The task details
     * @return The sync result
     */
    private SyncResult taskDetailsToSyncResult(JSONObject taskDetails) {
        try {
            JSONObject results = taskDetails.getJSONObject("result");
            JSONArray changesJSON = results.getJSONArray("changes");
            JSONArray conflictsJSON = results.getJSONArray("conflicts");
            String serializedLibrary = results.getString("library");
            SyncResult result = new SyncResult();
            for (int i = 0; i < changesJSON.length(); i++) {
                result.getChanges().add(changesJSON.getString(i));
            }
            for (int i = 0; i < conflictsJSON.length(); i++) {
                result.getConflicts().add(conflictsJSON.getString(i));
            }
            result.setSerializedLibrary(serializedLibrary);
            result.setSuccessful(true);
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the details of a task from it's status
     *
     * @param taskStatus The task status
     * @return The task details
     */
    private JSONObject getTaskDetails(JSONObject taskStatus) {
        try {
            return new JSONObject(taskStatus.getString("details"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if the sync is complete from a task status
     *
     * @param taskStatus The task status to check
     * @return Whether or not the sync is complete
     */
    private boolean isSyncComplete(JSONObject taskStatus, JSONObject taskDetails) {
        try {
            if (taskStatus.getBoolean("complete")) {
                return true;
            }
            if (taskDetails.has("error")) {
                throw new RuntimeException(
                        "Sync failed with error: '" + taskDetails.getString("error") + "'!");
            }
            return false;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the task status of a task
     *
     * @param taskId The task ID
     * @return The task status in JSON
     */
    private JSONObject getTaskStatus(long taskId) {
        try {
            String builtTaskEndpoint = taskEndpoint;
            if (!builtTaskEndpoint.endsWith("/")) {
                builtTaskEndpoint += "/";
            }
            return new JSONObject(
                    httpClient.getRequest(builtTaskEndpoint + taskId, Collections.<String, String>emptyMap()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception making HTTP request!");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start a sync
     *
     * @param oldLibrary The old library
     * @param newLibrary The new library
     * @return The id of the new sync task
     */
    private long trySync(String oldLibrary, String newLibrary) {
        JSONObject request = new JSONObject();
        try {
            request.put("old_library", oldLibrary);
            request.put("new_library", newLibrary);
            request.put("favorites_only", favoritesOnly);
            JSONObject results =
                    new JSONObject(
                            httpClient.postRequest(
                                    syncEndpoint, Collections.<String, String>emptyMap(), request.toString()));
            if (!results.getBoolean("success")) {
                throw new RuntimeException("Sync failed!");
            }
            return results.getLong("task_id");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception making HTTP request!");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        if (httpClient == null) {
            throw new NullPointerException("HttpClient cannot be null!");
        }
        this.httpClient = httpClient;
    }

    public String getSyncEndpoint() {
        return syncEndpoint;
    }

    public void setSyncEndpoint(String syncEndpoint) {
        if (syncEndpoint == null) {
            throw new NullPointerException("Sync endpoint cannot be null!");
        }
        this.syncEndpoint = syncEndpoint;
    }

    public int getProgressUpdateFrequency() {
        return progressUpdateFrequency;
    }

    public void setProgressUpdateFrequency(int progressUpdateFrequency) {
        this.progressUpdateFrequency = progressUpdateFrequency;
    }

    public boolean isFavoritesOnly() {
        return favoritesOnly;
    }

    public void setFavoritesOnly(boolean favoritesOnly) {
        this.favoritesOnly = favoritesOnly;
    }
}
