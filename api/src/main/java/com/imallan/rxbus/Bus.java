package com.imallan.rxbus;

import com.imallan.rxbus.annotation.RxBusSchedulers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

@SuppressWarnings("unused")
public class Bus {

    private static Bus sBus;

    private PublishSubject mPublishSubject;
    private Observable mObservable;
    private ConcurrentMap<String, Object> mPersistEventMap;
    private ConcurrentMap<Object, CompositeSubscription> mCompositeSubscriptionMap;

    private Bus() {
        mPersistEventMap = new ConcurrentHashMap<>();
        mCompositeSubscriptionMap = new ConcurrentHashMap<>();
        mPublishSubject = PublishSubject.create();
        //noinspection unchecked
        mObservable = mPublishSubject.onBackpressureLatest()
                .subscribeOn(Schedulers.io());
//                .observeOn(AndroidSchedulers.mainThread());
    }

    public static Bus getInstance() {
        synchronized (Bus.class) {
            if (sBus == null) {
                synchronized (Bus.class) {
                    sBus = new Bus();
                }
            }
        }
        return sBus;
    }

    public <T> void subscribe(
            Object target,
            final Class<T> clazz,
            Action1<T> action,
            int schedulerInt) {
        Scheduler scheduler;
        switch (schedulerInt) {
            case RxBusSchedulers.COMPUTATION:
                scheduler = Schedulers.computation();
                break;
            case RxBusSchedulers.IMMEDIATE:
                scheduler = Schedulers.immediate();
                break;
            case RxBusSchedulers.IO:
                scheduler = Schedulers.io();
                break;
            case RxBusSchedulers.NEW_THREAD:
                scheduler = Schedulers.newThread();
                break;
            default:
            case RxBusSchedulers.ANDROID_MAIN:
                scheduler = AndroidSchedulers.mainThread();
                break;
        }
        subscribeInternal(target, clazz, action, scheduler);
    }

    private <T> void subscribeInternal(
            Object target,
            final Class<T> clazz,
            Action1<T> action,
            Scheduler scheduler
    ) {
        CompositeSubscription compositeSubscription = mCompositeSubscriptionMap.get(target);
        if (compositeSubscription == null) {
            compositeSubscription = new CompositeSubscription();
            mCompositeSubscriptionMap.put(target, compositeSubscription);
        }
        Subscription subscription = subscribeInternal(clazz, action, scheduler);
        compositeSubscription.add(subscription);
    }

    private <T> Subscription subscribeInternal(
            final Class<T> clazz,
            Action1<T> action,
            Scheduler scheduler
    ) {
        //noinspection unchecked
        Subscription subscription = mObservable
                .filter(new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object event) {
                        return event.getClass().getCanonicalName().equals(clazz.getCanonicalName());
                    }
                })
                .observeOn(scheduler)
                .subscribe(action);
        if (mPersistEventMap.containsKey(clazz.getCanonicalName())) {
            //noinspection unchecked
            action.call((T) mPersistEventMap.get(clazz.getCanonicalName()));
        }
        return subscription;
    }

    public static <T> void send(T t) {
        getInstance().sendInternal(t);
    }

    public static <T> void sendPersist(T t) {
        getInstance().sendPersistInternal(t);
    }

    public static <T> void removePersist(Class<T> clazz) {
        getInstance().mPersistEventMap.remove(clazz.getName());
    }

    private <T> void sendInternal(T t) {
        //noinspection unchecked
        mPublishSubject.onNext(t);
    }

    private <T> void sendPersistInternal(T t) {
        //noinspection unchecked
        mPublishSubject.onNext(t);
        mPersistEventMap.put(t.getClass().getCanonicalName(), t);
    }

    /**
     * @param target binding target
     * @deprecated consider using {Target}$BindUtils to avoid reflection
     */
    @Deprecated
    public static void bind(Object target) {
        try {
            Class<?> clazz = Class.forName(target.getClass().getCanonicalName() + "$BindUtils");
            clazz.getDeclaredMethod("bind", target.getClass()).invoke(null, target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void unbind(Object target) {
        getInstance().unbindInternal(target);
    }

    private void unbindInternal(Object target) {
        CompositeSubscription subscription = mCompositeSubscriptionMap.get(target);
        if (subscription != null) subscription.clear();
        mCompositeSubscriptionMap.remove(target);
    }

}
