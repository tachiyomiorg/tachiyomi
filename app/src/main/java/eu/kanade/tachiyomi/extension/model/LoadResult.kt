package eu.kanade.tachiyomi.extension.model

sealed class LoadResult {

    class Success(val extension: Extension.Installed) : LoadResult()
    class Untrusted(val extension: Extension.Untrusted) : LoadResult()
    class Error(val message: String? = null) : LoadResult() {
        constructor(exception: Throwable) : this(exception.message)
    }
}
