package eu.kanade.tachiyomi.ui.base.presenter;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nucleus.presenter.Presenter;
import nucleus.presenter.delivery.DeliverFirst;
import nucleus.presenter.delivery.DeliverLatestCache;
import nucleus.presenter.delivery.DeliverReplay;
import nucleus.presenter.delivery.Delivery;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.internal.util.SubscriptionList;
import rx.subjects.BehaviorSubject;

/**
 * This is an extension of {@link Presenter} which provides RxJava functionality.
 *
 * @param <View> a type of view.
 */
public class RxPresenter<View> extends Presenter<View> {

    private static final String REQUESTED_KEY = RxPresenter.class.getName() + "#requested";

    private final BehaviorSubject<View> views = BehaviorSubject.create();
    private final SubscriptionList subscriptions = new SubscriptionList();

    private final HashMap<Integer, Func0<Subscription>> restartables = new HashMap<>();
    private final HashMap<Integer, Subscription> restartableSubscriptions = new HashMap<>();
    private final ArrayList<Integer> requested = new ArrayList<>();

    /**
     * Returns an {@link rx.Observable} that emits the current attached view or null.
     * See {@link BehaviorSubject} for more information.
     *
     * @return an observable that emits the current attached view or null.
     */
    public Observable<View> view() {
        return views;
    }

    /**
     * Registers a subscription to automatically unsubscribe it during onDestroy.
     * See {@link SubscriptionList#add(Subscription) for details.}
     *
     * @param subscription a subscription to add.
     */
    public void add(Subscription subscription) {
        subscriptions.add(subscription);
    }

    /**
     * Removes and unsubscribes a subscription that has been registered with {@link #add} previously.
     * See {@link SubscriptionList#remove(Subscription)} for details.
     *
     * @param subscription a subscription to remove.
     */
    public void remove(Subscription subscription) {
        subscriptions.remove(subscription);
    }

    /**
     * A restartable is any RxJava observable that can be started (subscribed) and
     * should be automatically restarted (re-subscribed) after a process restart if
     * it was still subscribed at the moment of saving presenter's state.
     *
     * Registers a factory. Re-subscribes the restartable after the process restart.
     *
     * @param restartableId id of the restartable
     * @param factory       factory of the restartable
     */
    public void restartable(int restartableId, Func0<Subscription> factory) {
        restartables.put(restartableId, factory);
        if (requested.contains(restartableId))
            start(restartableId);
    }

    /**
     * Starts the given restartable.
     *
     * @param restartableId id of the restartable
     */
    public void start(int restartableId) {
        stop(restartableId);
        requested.add(restartableId);
        restartableSubscriptions.put(restartableId, restartables.get(restartableId).call());
    }

    /**
     * Unsubscribes a restartable
     *
     * @param restartableId id of a restartable.
     */
    public void stop(int restartableId) {
        requested.remove((Integer) restartableId);
        Subscription subscription = restartableSubscriptions.get(restartableId);
        if (subscription != null)
            subscription.unsubscribe();
    }

    /**
     * Checks if a restartable is unsubscribed.
     *
     * @param restartableId id of the restartable.
     * @return true if the subscription is null or unsubscribed, false otherwise.
     */
    public boolean isUnsubscribed(int restartableId) {
        return isUnsubscribed(restartableSubscriptions.get(restartableId));
    }

    /**
     * Checks if a subscription is unsubscribed.
     *
     * @param subscription the subscription to check.
     * @return true if the subscription is null or unsubscribed, false otherwise.
     */
    public boolean isUnsubscribed(@Nullable Subscription subscription) {
        return subscription == null || subscription.isUnsubscribed();
    }

    /**
     * This is a shortcut that can be used instead of combining together
     * {@link #restartable(int, Func0)},
     * {@link #deliverFirst()},
     * {@link #split(Action2, Action2)}.
     *
     * @param restartableId     an id of the restartable.
     * @param observableFactory a factory that should return an Observable when the restartable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     * @param onError           a callback that will be called if the source observable emits onError.
     * @param <T>               the type of the observable.
     */
    public <T> void restartableFirst(int restartableId, final Func0<Observable<T>> observableFactory,
        final Action2<View, T> onNext, @Nullable final Action2<View, Throwable> onError) {

        restartable(restartableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return observableFactory.call()
                    .compose(RxPresenter.this.<T>deliverFirst())
                    .subscribe(split(onNext, onError));
            }
        });
    }

    /**
     * This is a shortcut for calling {@link #restartableFirst(int, Func0, Action2, Action2)} with the last parameter = null.
     */
    public <T> void restartableFirst(int restartableId, final Func0<Observable<T>> observableFactory, final Action2<View, T> onNext) {
        restartableFirst(restartableId, observableFactory, onNext, null);
    }

    /**
     * This is a shortcut that can be used instead of combining together
     * {@link #restartable(int, Func0)},
     * {@link #deliverLatestCache()},
     * {@link #split(Action2, Action2)}.
     *
     * @param restartableId     an id of the restartable.
     * @param observableFactory a factory that should return an Observable when the restartable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     * @param onError           a callback that will be called if the source observable emits onError.
     * @param <T>               the type of the observable.
     */
    public <T> void restartableLatestCache(int restartableId, final Func0<Observable<T>> observableFactory,
        final Action2<View, T> onNext, @Nullable final Action2<View, Throwable> onError) {

        restartable(restartableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return observableFactory.call()
                    .compose(RxPresenter.this.<T>deliverLatestCache())
                    .subscribe(split(onNext, onError));
            }
        });
    }

    /**
     * This is a shortcut for calling {@link #restartableLatestCache(int, Func0, Action2, Action2)} with the last parameter = null.
     */
    public <T> void restartableLatestCache(int restartableId, final Func0<Observable<T>> observableFactory, final Action2<View, T> onNext) {
        restartableLatestCache(restartableId, observableFactory, onNext, null);
    }

    /**
     * This is a shortcut that can be used instead of combining together
     * {@link #restartable(int, Func0)},
     * {@link #deliverReplay()},
     * {@link #split(Action2, Action2)}.
     *
     * @param restartableId     an id of the restartable.
     * @param observableFactory a factory that should return an Observable when the restartable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     * @param onError           a callback that will be called if the source observable emits onError.
     * @param <T>               the type of the observable.
     */
    public <T> void restartableReplay(int restartableId, final Func0<Observable<T>> observableFactory,
        final Action2<View, T> onNext, @Nullable final Action2<View, Throwable> onError) {

        restartable(restartableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return observableFactory.call()
                    .compose(RxPresenter.this.<T>deliverReplay())
                    .subscribe(split(onNext, onError));
            }
        });
    }

    /**
     * This is a shortcut for calling {@link #restartableReplay(int, Func0, Action2, Action2)} with the last parameter = null.
     */
    public <T> void restartableReplay(int restartableId, final Func0<Observable<T>> observableFactory, final Action2<View, T> onNext) {
        restartableReplay(restartableId, observableFactory, onNext, null);
    }

    /**
     * A startable behaves the same as a restartable but it does not resubscribe on process restart
     *
     * @param startableId       an id of the restartable.
     * @param observableFactory a factory that should return an Observable when the startable should run.
     */
    public <T> void startable(int startableId, final Func0<Observable<T>> observableFactory) {
        restartables.put(startableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {return observableFactory.call().subscribe();}
        });
    }

    /**
     * A startable behaves the same as a restartable but it does not resubscribe on process restart
     *
     * @param startableId       an id of the restartable.
     * @param observableFactory a factory that should return an Observable when the startable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     * @param onError           a callback that will be called if the source observable emits onError.
     */
    public <T> void startable(int startableId, final Func0<Observable<T>> observableFactory,
        final Action1<T> onNext, final Action1<Throwable> onError) {

        restartables.put(startableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {return observableFactory.call().subscribe(onNext, onError);}
        });
    }

    /**
     * A startable behaves the same as a restartable but it does not resubscribe on process restart
     *
     * @param startableId       an id of the restartable.
     * @param observableFactory a factory that should return an Observable when the startable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     */
    public <T> void startable(int startableId, final Func0<Observable<T>> observableFactory, final Action1<T> onNext) {
        restartables.put(startableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {return observableFactory.call().subscribe(onNext);}
        });
    }
    
    /**
     * This is a shortcut that can be used instead of combining together
     * {@link #startable(int, Func0)},
     * {@link #deliverFirst()},
     * {@link #split(Action2, Action2)}.
     *
     * @param startableId       an id of the startable.
     * @param observableFactory a factory that should return an Observable when the startable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     * @param onError           a callback that will be called if the source observable emits onError.
     * @param <T>               the type of the observable.
     */
    public <T> void startableFirst(int startableId, final Func0<Observable<T>> observableFactory,
        final Action2<View, T> onNext, @Nullable final Action2<View, Throwable> onError) {

        restartables.put(startableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return observableFactory.call()
                        .compose(RxPresenter.this.<T>deliverFirst())
                        .subscribe(split(onNext, onError));
            }
        });
    }

    /**
     * This is a shortcut for calling {@link #startableFirst(int, Func0, Action2, Action2)} with the last parameter = null.
     */
    public <T> void startableFirst(int startableId, final Func0<Observable<T>> observableFactory, final Action2<View, T> onNext) {
        startableFirst(startableId, observableFactory, onNext, null);
    }

    /**
     * This is a shortcut that can be used instead of combining together
     * {@link #startable(int, Func0)},
     * {@link #deliverLatestCache()},
     * {@link #split(Action2, Action2)}.
     *
     * @param startableId       an id of the startable.
     * @param observableFactory a factory that should return an Observable when the startable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     * @param onError           a callback that will be called if the source observable emits onError.
     * @param <T>               the type of the observable.
     */
    public <T> void startableLatestCache(int startableId, final Func0<Observable<T>> observableFactory,
        final Action2<View, T> onNext, @Nullable final Action2<View, Throwable> onError) {

        restartables.put(startableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return observableFactory.call()
                        .compose(RxPresenter.this.<T>deliverLatestCache())
                        .subscribe(split(onNext, onError));
            }
        });
    }

    /**
     * This is a shortcut for calling {@link #startableLatestCache(int, Func0, Action2, Action2)} with the last parameter = null.
     */
    public <T> void startableLatestCache(int startableId, final Func0<Observable<T>> observableFactory, final Action2<View, T> onNext) {
        startableLatestCache(startableId, observableFactory, onNext, null);
    }

    /**
     * This is a shortcut that can be used instead of combining together
     * {@link #startable(int, Func0)},
     * {@link #deliverReplay()},
     * {@link #split(Action2, Action2)}.
     *
     * @param startableId       an id of the startable.
     * @param observableFactory a factory that should return an Observable when the startable should run.
     * @param onNext            a callback that will be called when received data should be delivered to view.
     * @param onError           a callback that will be called if the source observable emits onError.
     * @param <T>               the type of the observable.
     */
    public <T> void startableReplay(int startableId, final Func0<Observable<T>> observableFactory,
        final Action2<View, T> onNext, @Nullable final Action2<View, Throwable> onError) {

        restartables.put(startableId, new Func0<Subscription>() {
            @Override
            public Subscription call() {
                return observableFactory.call()
                        .compose(RxPresenter.this.<T>deliverReplay())
                        .subscribe(split(onNext, onError));
            }
        });
    }

    /**
     * This is a shortcut for calling {@link #startableReplay(int, Func0, Action2, Action2)} with the last parameter = null.
     */
    public <T> void startableReplay(int startableId, final Func0<Observable<T>> observableFactory, final Action2<View, T> onNext) {
        startableReplay(startableId, observableFactory, onNext, null);
    }

    /**
     * Returns an {@link rx.Observable.Transformer} that couples views with data that has been emitted by
     * the source {@link rx.Observable}.
     *
     * {@link #deliverLatestCache} keeps the latest onNext value and emits it each time a new view gets attached.
     * If a new onNext value appears while a view is attached, it will be delivered immediately.
     *
     * @param <T> the type of source observable emissions
     */
    public <T> DeliverLatestCache<View, T> deliverLatestCache() {
        return new DeliverLatestCache<>(views);
    }

    /**
     * Returns an {@link rx.Observable.Transformer} that couples views with data that has been emitted by
     * the source {@link rx.Observable}.
     *
     * {@link #deliverFirst} delivers only the first onNext value that has been emitted by the source observable.
     *
     * @param <T> the type of source observable emissions
     */
    public <T> DeliverFirst<View, T> deliverFirst() {
        return new DeliverFirst<>(views);
    }

    /**
     * Returns an {@link rx.Observable.Transformer} that couples views with data that has been emitted by
     * the source {@link rx.Observable}.
     *
     * {@link #deliverReplay} keeps all onNext values and emits them each time a new view gets attached.
     * If a new onNext value appears while a view is attached, it will be delivered immediately.
     *
     * @param <T> the type of source observable emissions
     */
    public <T> DeliverReplay<View, T> deliverReplay() {
        return new DeliverReplay<>(views);
    }

    /**
     * Returns a method that can be used for manual restartable chain build. It returns an Action1 that splits
     * a received {@link Delivery} into two {@link Action2} onNext and onError calls.
     *
     * @param onNext  a method that will be called if the delivery contains an emitted onNext value.
     * @param onError a method that will be called if the delivery contains an onError throwable.
     * @param <T>     a type on onNext value.
     * @return an Action1 that splits a received {@link Delivery} into two {@link Action2} onNext and onError calls.
     */
    public <T> Action1<Delivery<View, T>> split(final Action2<View, T> onNext, @Nullable final Action2<View, Throwable> onError) {
        return new Action1<Delivery<View, T>>() {
            @Override
            public void call(Delivery<View, T> delivery) {
                delivery.split(onNext, onError);
            }
        };
    }

    /**
     * This is a shortcut for calling {@link #split(Action2, Action2)} when the second parameter is null.
     */
    public <T> Action1<Delivery<View, T>> split(Action2<View, T> onNext) {
        return split(onNext, null);
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    protected void onCreate(Bundle savedState) {
        if (savedState != null)
            requested.addAll(savedState.getIntegerArrayList(REQUESTED_KEY));
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    protected void onDestroy() {
        views.onCompleted();
        subscriptions.unsubscribe();
        for (Map.Entry<Integer, Subscription> entry : restartableSubscriptions.entrySet())
            entry.getValue().unsubscribe();
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    protected void onSave(Bundle state) {
        for (int i = requested.size() - 1; i >= 0; i--) {
            int restartableId = requested.get(i);
            Subscription subscription = restartableSubscriptions.get(restartableId);
            if (subscription != null && subscription.isUnsubscribed())
                requested.remove(i);
        }
        state.putIntegerArrayList(REQUESTED_KEY, requested);
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    protected void onTakeView(View view) {
        views.onNext(view);
    }

    /**
     * {@inheritDoc}
     */
    @CallSuper
    @Override
    protected void onDropView() {
        views.onNext(null);
    }

    /**
     * Please, use restartableXX and deliverXX methods for pushing data from RxPresenter into View.
     */
    @Deprecated
    @Nullable
    @Override
    public View getView() {
        return super.getView();
    }
}
