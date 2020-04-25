package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import rx.Observable
import rx.Subscription
import rx.subscriptions.CompositeSubscription

abstract class RxController<VB : ViewBinding>(bundle: Bundle? = null) : BaseController<VB>(bundle) {

    var untilDetachSubscriptions = CompositeSubscription()
        private set

    var untilDestroySubscriptions = CompositeSubscription()
        private set

    @CallSuper
    override fun onAttach(view: View) {
        super.onAttach(view)
        if (untilDetachSubscriptions.isUnsubscribed) {
            untilDetachSubscriptions = CompositeSubscription()
        }
    }

    @CallSuper
    override fun onDetach(view: View) {
        super.onDetach(view)
        untilDetachSubscriptions.unsubscribe()
    }

    @CallSuper
    override fun onViewCreated(view: View) {
        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }
    }

    @CallSuper
    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        untilDestroySubscriptions.unsubscribe()
    }

    fun <T> Observable<T>.subscribeUntilDetach(): Subscription {
        return subscribe().also { untilDetachSubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDetach(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDetachSubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDetach(
        onNext: (T) -> Unit,
        onError: (Throwable) -> Unit
    ): Subscription {
        return subscribe(onNext, onError).also { untilDetachSubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDetach(
        onNext: (T) -> Unit,
        onError: (Throwable) -> Unit,
        onCompleted: () -> Unit
    ): Subscription {
        return subscribe(onNext, onError, onCompleted).also { untilDetachSubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDestroy(): Subscription {
        return subscribe().also { untilDestroySubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDestroy(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDestroySubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDestroy(
        onNext: (T) -> Unit,
        onError: (Throwable) -> Unit
    ): Subscription {
        return subscribe(onNext, onError).also { untilDestroySubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDestroy(
        onNext: (T) -> Unit,
        onError: (Throwable) -> Unit,
        onCompleted: () -> Unit
    ): Subscription {
        return subscribe(onNext, onError, onCompleted).also { untilDestroySubscriptions.add(it) }
    }
}
