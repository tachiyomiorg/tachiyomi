package xyz.nulldev.ts.sync.model

import java.util.ArrayList

/**
 * Project: TachiServer
 * Author: nulldev
 * Creation Date: 26/08/16

 * A simple sync result
 */
sealed class SyncResult() {
    class Progress(val status: String = ""): SyncResult()
    class Fail(val error: String = ""): SyncResult()
    class Success(val changes: List<String> = ArrayList(),
                  val conflicts: List<String> = ArrayList(),
                  val serializedLibrary: String): SyncResult()
}
