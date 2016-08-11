package com.imallan.rxbusexample;

import android.app.Application;

import com.imallan.rxbus.Bus;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

public class MyApplication extends Application {

    private Subscription mSubscription;

    @Override
    public void onCreate() {
        super.onCreate();
        mSubscription = Observable.interval(1, TimeUnit.SECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        Bus.send(new MainActivity.MyEvent(aLong.toString()));
                        Bus.send(new MyView.ViewEvent(aLong.toString()));
                    }
                });
        Bus.sendPersist(new MainActivity.MyEvent("Starting"));
    }

    @Override
    public void onTerminate() {
        mSubscription.unsubscribe();
        super.onTerminate();
    }
}
