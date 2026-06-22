package com.example.attendance;

import android.app.Application;
import android.content.Context;

/** 持有全局 Context，供 SessionManager / ApiClient 读取 token。 */
public class App extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context get() {
        return appContext;
    }
}
