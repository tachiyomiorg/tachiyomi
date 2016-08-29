package xyz.nulldev.ts.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * Project: TachiServer
 * Author: nulldev
 * Creation Date: 26/08/16
 *
 * A simple sync result
 */
public class SyncResult {
    private boolean successful;
    private String error;
    private List<String> changes = new ArrayList<>();
    private List<String> conflicts = new ArrayList<>();
    private String serializedLibrary;

    public List<String> getChanges() {
        return changes;
    }

    public void setChanges(List<String> changes) {
        this.changes = changes;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }

    public String getSerializedLibrary() {
        return serializedLibrary;
    }

    public void setSerializedLibrary(String serializedLibrary) {
        this.serializedLibrary = serializedLibrary;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public static SyncResult fail() {
        return fail(null);
    }

    public static SyncResult fail(String error) {
        SyncResult syncResult = new SyncResult();
        syncResult.setSuccessful(false);
        syncResult.setError(error);
        return syncResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncResult that = (SyncResult) o;

        return successful == that.successful
                && (error != null
                        ? error.equals(that.error)
                        : that.error == null
                                && (changes != null
                                        ? changes.equals(that.changes)
                                        : that.changes == null
                                                && (conflicts != null
                                                        ? conflicts.equals(that.conflicts)
                                                        : that.conflicts == null
                                                                && (serializedLibrary != null
                                                                        ? serializedLibrary.equals(
                                                                                that.serializedLibrary)
                                                                        : that.serializedLibrary
                                                                                == null))));
    }

    @Override
    public int hashCode() {
        int result = (successful ? 1 : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + (changes != null ? changes.hashCode() : 0);
        result = 31 * result + (conflicts != null ? conflicts.hashCode() : 0);
        result = 31 * result + (serializedLibrary != null ? serializedLibrary.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SyncResult{" +
                "successful=" + successful +
                ", error='" + error + '\'' +
                ", changes=" + changes +
                ", conflicts=" + conflicts +
                ", serializedLibrary='" + serializedLibrary + '\'' +
                '}';
    }
}
