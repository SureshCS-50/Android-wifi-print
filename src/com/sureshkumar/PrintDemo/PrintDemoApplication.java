package com.sureshkumar.PrintDemo;

import android.app.Application;
import android.content.Context;

public class PrintDemoApplication extends Application {

    public static final String CONTENT_UPDATE_CHANNEL = "Testing";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {

        super.onCreate();

        ObservableSingleton.initInstance();
    }

}
