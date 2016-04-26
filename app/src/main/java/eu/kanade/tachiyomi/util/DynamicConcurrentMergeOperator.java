package eu.kanade.tachiyomi.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class DynamicConcurrentMergeOperator<T, R> implements Operator<R, T> {
    private final Func1<? super T, ? extends Observable<? extends R>> mapper;
    private final Observable<Integer> workerCount;

    public DynamicConcurrentMergeOperator(
            Func1<? super T, ? extends Observable<? extends R>> mapper,
            Observable<Integer> workerCount) {
        this.mapper = mapper;
        this.workerCount = workerCount;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> t) {
        DynamicConcurrentMerge<T, R> parent = new DynamicConcurrentMerge<>(t, mapper);
        t.add(parent);
        parent.init(workerCount);

        return parent;
    }

    static final class DynamicConcurrentMerge<T, R> extends Subscriber<T> {
        private final Subscriber<? super R> actual;
        private final Func1<? super T, ? extends Observable<? extends R>> mapper;
        private final Queue<T> queue;
        private final CopyOnWriteArrayList<DynamicWorker<T, R>> workers;
        private final CompositeSubscription composite;
        private final AtomicInteger wipActive;
        private final AtomicBoolean once;
        private long id;

        public DynamicConcurrentMerge(Subscriber<? super R> actual,
                                      Func1<? super T, ? extends Observable<? extends R>> mapper) {
            this.actual = actual;
            this.mapper = mapper;
            this.queue = new ConcurrentLinkedQueue<>();
            this.workers = new CopyOnWriteArrayList<>();
            this.composite = new CompositeSubscription();
            this.wipActive = new AtomicInteger(1);
            this.once = new AtomicBoolean();
            this.add(composite);
            this.request(0);
        }

        public void init(Observable<Integer> workerCount) {
            Subscription wc = workerCount.subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer n) {
                    int n0 = workers.size();
                    if (n0 < n) {
                        for (int i = n0; i < n; i++) {
                            DynamicWorker<T, R> dw = new DynamicWorker<>(++id, DynamicConcurrentMerge.this);
                            workers.add(dw);
                            DynamicConcurrentMerge.this.request(1);
                            dw.tryNext();
                        }
                    } else if (n0 > n) {
                        for (int i = 0; i < n; i++) {
                            workers.get(i).start();
                        }

                        for (int i = n0 - 1; i >= n; i--) {
                            workers.get(i).stop();
                        }
                    }

                    if (!once.get() && once.compareAndSet(false, true)) {
                        DynamicConcurrentMerge.this.request(n);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable e) {DynamicConcurrentMerge.this.onError(e);}
            });

            composite.add(wc);
        }

        void requestMore(long n) {
            request(n);
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            wipActive.getAndIncrement();
            for (DynamicWorker<T, R> w : workers) {
                w.tryNext();
            }
        }

        @Override
        public void onError(Throwable e) {
            composite.unsubscribe();
            actual.onError(e);
        }

        @Override
        public void onCompleted() {
            if (wipActive.decrementAndGet() == 0) {
                actual.onCompleted();
            }
        }
    }

    static final class DynamicWorker<T, R> {
        private final long id;
        private final AtomicBoolean running;
        private final DynamicConcurrentMerge<T, R> parent;
        private final AtomicBoolean stop;

        public DynamicWorker(long id, DynamicConcurrentMerge<T, R> parent) {
            this.id = id;
            this.parent = parent;
            this.stop = new AtomicBoolean();
            this.running = new AtomicBoolean();
        }

        public void tryNext() {
            if (!running.get() && running.compareAndSet(false, true)) {
                T t;
                if (stop.get()) {
                    parent.workers.remove(this);
                    return;
                }
                t = parent.queue.poll();
                if (t == null) {
                    running.set(false);
                    return;
                }

                Observable<? extends R> out = parent.mapper.call(t);

                final Subscriber<R> s = new Subscriber<R>() {
                    @Override
                    public void onNext(R t) {
                        parent.actual.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        parent.onError(e);
                    }

                    @Override
                    public void onCompleted() {
                        parent.onCompleted();
                        if (parent.wipActive.get() != 0) {
                            running.set(false);
                            parent.requestMore(1);
                            tryNext();
                        }
                    }
                };

                parent.composite.add(s);
                s.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {parent.composite.remove(s);}
                }));

                out.subscribe(s);
            }
        }

        public void start() {
            stop.set(false);
            tryNext();
        }

        public void stop() {
            stop.set(true);
            if (running.compareAndSet(false, true)) {
                parent.workers.remove(this);
            }
        }
    }

}