package xyz.nulldev.ts.sync.model

import java.util.ArrayList

/**
 * A simple sync result
 */
sealed class SyncResult() {
    class Progress(val status: String = ""): SyncResult()
    class Fail(val error: String? = null): SyncResult()
    class Success(val changes: List<String> = ArrayList(),
                  val conflicts: List<String> = ArrayList(),
                  val serializedLibrary: String): SyncResult()
}
