package com.sureshkumar.PrintDemo;

import android.app.Application;

public class PrintDemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initSingleton();
    }

    private void initSingleton(){
        ObservableSingleton.initInstance();
    }

}
