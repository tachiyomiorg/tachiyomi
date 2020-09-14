package eu.kanade.tachiyomi.util.lang

import rx.Observable
import rx.Scheduler
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit.MILLISECONDS

class RetryWithDelay(
    private val maxRetries: Int = 1,
    private val retryStrategy: (Int) -> Int = { 1000 },
    private val scheduler: Scheduler = Schedulers.computation()
) : Func1<Observable<out Throwable>, Observable<*>> {

    private var retryCount = 0

    override fun call(attempts: Observable<out Throwable>) = attempts.flatMap { error ->
        val count = ++retryCount
        if (count <= maxRetries) {
            Observable.timer(retryStrategy(count).toLong(), MILLISECONDS, scheduler)
        } else {
            Observable.error(error as Throwable)
        }
    }
}
