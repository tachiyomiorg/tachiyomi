package eu.kanade.tachiyomi.data.updater

abstract class UpdateResult {

    open class NewUpdate<T : Release>(val release: T): UpdateResult()
    open class NoNewUpdate: UpdateResult()

}
