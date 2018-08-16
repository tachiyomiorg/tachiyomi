package eu.kanade.tachiyomi.util

import rx.Observable
import rx.Subscription
import rx.subscriptions.CompositeSubscription

fun Subscription?.isNullOrUnsubscribed() = this == null || isUnsubscribed

operator fun CompositeSubscription.plusAssign(subscription: Subscription) = add(subscription)

fun <T, U, R> Observable<T>.combineLatest(o2: Observable<U>, combineFn: (T, U) -> R): Observable<R> {
    return Observable.combineLatest(this, o2, combineFn)
}

fun Subscription.addTo(subscriptions: CompositeSubscription) {
    subscriptions.add(this)
}
