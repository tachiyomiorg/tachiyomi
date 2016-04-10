package eu.kanade.tachiyomi.ui.base.presenter

import android.content.Context
import nucleus.view.ViewWithPresenter
import rx.Observable

open class BasePresenter<V : ViewWithPresenter<*>> : RxPresenter<V>() {

    lateinit var context: Context

    /**
     * Subscribes an observable with [deliverFirst] and adds it to the presenter's lifecycle
     * subscription list.
     *
     * @param onNext function to execute when the observable emits an item.
     * @param onError function to execute when the observable throws an error.
     */
    fun <T> Observable<T>.subscribeFirst(onNext: (V, T) -> Unit, onError: ((V, Throwable) -> Unit)? = null)
            = compose(deliverFirst<T>()).subscribe(split(onNext, onError)).apply { add(this) }

    /**
     * Subscribes an observable with [deliverLatestCache] and adds it to the presenter's lifecycle
     * subscription list.
     *
     * @param onNext function to execute when the observable emits an item.
     * @param onError function to execute when the observable throws an error.
     */
    fun <T> Observable<T>.subscribeLatestCache(onNext: (V, T) -> Unit, onError: ((V, Throwable) -> Unit)? = null)
            = compose(deliverLatestCache<T>()).subscribe(split(onNext, onError)).apply { add(this) }

    /**
     * Subscribes an observable with [deliverReplay] and adds it to the presenter's lifecycle
     * subscription list.
     *
     * @param onNext function to execute when the observable emits an item.
     * @param onError function to execute when the observable throws an error.
     */
    fun <T> Observable<T>.subscribeReplay(onNext: (V, T) -> Unit, onError: ((V, Throwable) -> Unit)? = null)
            = compose(deliverReplay<T>()).subscribe(split(onNext, onError)).apply { add(this) }

}
