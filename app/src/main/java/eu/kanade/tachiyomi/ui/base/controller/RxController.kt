package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.support.annotation.CallSuper
import android.view.View
import rx.Observable
import rx.Subscription
import rx.exceptions.OnErrorNotImplementedException
import rx.subscriptions.CompositeSubscription

abstract class RxController(bundle: Bundle? = null) : BaseController(bundle) {

    private var untilDetachSubscriptions = CompositeSubscription()

    private var untilDestroySubscriptions = CompositeSubscription()

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

    fun <T> Observable<T>.subscribeUntilDetach(
            onNext: (T) -> Unit = emptyNext,
            onError: (Throwable) -> Unit = emptyError,
            onCompleted: () -> Unit = emptyCompleted): Subscription {

        return subscribe(onNext, onError, onCompleted).also { untilDetachSubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDestroy(
            onNext: (T) -> Unit = emptyNext,
            onError: (Throwable) -> Unit = emptyError,
            onCompleted: () -> Unit = emptyCompleted): Subscription {

        return subscribe(onNext, onError, onCompleted).also { untilDestroySubscriptions.add(it) }
    }

    private companion object {
        val emptyNext: (Any?) -> Unit = {}
        val emptyError: (Throwable) -> Unit = { throw OnErrorNotImplementedException(it) }
        val emptyCompleted = {}
    }

}