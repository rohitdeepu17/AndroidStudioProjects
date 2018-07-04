package com.example.codeworld.gridviewexample;

import android.app.Application;

/**
 * Created by rohitsingla on 04/07/18.
 */

public class MyApplication extends Application{
    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    private static boolean activityVisible;
}
