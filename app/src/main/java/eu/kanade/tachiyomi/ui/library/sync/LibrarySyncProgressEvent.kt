package eu.kanade.tachiyomi.ui.library.sync

sealed class LibrarySyncProgressEvent {
    class ProgressUpdated(val status: String) : LibrarySyncProgressEvent()
    class Completed(val conflicts: List<String>) : LibrarySyncProgressEvent()
    class Error(val error: String) : LibrarySyncProgressEvent()
}