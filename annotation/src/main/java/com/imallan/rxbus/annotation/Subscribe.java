package com.imallan.rxbus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.imallan.rxbus.annotation.RxBusSchedulers.ANDROID_MAIN;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Subscribe {

    int scheduler() default ANDROID_MAIN;

}

