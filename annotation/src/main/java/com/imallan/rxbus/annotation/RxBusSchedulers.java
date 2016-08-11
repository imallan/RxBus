package com.imallan.rxbus.annotation;

public interface RxBusSchedulers {
    int IMMEDIATE = 0;
    int IO = 1;
    int COMPUTATION = 2;
    int ANDROID_MAIN = 3;
    int NEW_THREAD = 4;
}
