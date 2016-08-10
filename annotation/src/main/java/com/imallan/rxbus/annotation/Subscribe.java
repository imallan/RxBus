package com.imallan.rxbus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Subscribe {

    int IMMEDIATE = 0;
    int IO = 1;
    int COMPUTATION = 2;
    int ANDROID_MAIN = 3;
    int NEW_THREAD = 4;


    int scheduler() default ANDROID_MAIN;

}

