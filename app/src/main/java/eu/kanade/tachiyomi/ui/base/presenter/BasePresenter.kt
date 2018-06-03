package eu.kanade.tachiyomi.ui.base.presenter

import android.os.Bundle
import nucleus.presenter.RxPresenter
import nucleus.presenter.delivery.Delivery
import rx.Observable

open class BasePresenter<V> : RxPresenter<V>() {

    override fun onCreate(savedState: Bundle?) {
        try {
            super.onCreate(savedState)
        } catch (e: NullPointerException) {
            // Swallow this error. This should be fixed in the library but since it's not critical
            // (only used by restartables) it should be enough. It saves me a fork.
        }
    }

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

    /**
     * Subscribes an observable with [DeliverWithView] and adds it to the presenter's lifecycle
     * subscription list.
     *
     * @param onNext function to execute when the observable emits an item.
     * @param onError function to execute when the observable throws an error.
     */
    fun <T> Observable<T>.subscribeWithView(onNext: (V, T) -> Unit, onError: ((V, Throwable) -> Unit)? = null)
            = compose(DeliverWithView<V, T>(view())).subscribe(split(onNext, onError)).apply { add(this) }

    /**
     * A deliverable that only emits to the view if attached, otherwise the event is ignored.
     */
    class DeliverWithView<View, T>(private val view: Observable<View>) : Observable.Transformer<T, Delivery<View, T>> {

        override fun call(observable: Observable<T>): Observable<Delivery<View, T>> {
            return observable
                    .materialize()
                    .filter { notification -> !notification.isOnCompleted }
                    .flatMap { notification ->
                        view.take(1).filter { it != null }.map { Delivery(it, notification) }
                    }
        }
    }

}
