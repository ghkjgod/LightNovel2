package com.ghkjgod.lightnovel;

import android.app.Application;
import android.content.Context;

import com.facebook.drawee.backends.pipeline.Fresco;

public class MyApp extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        Fresco.initialize(this);
    }

    public static Context getContext(){
        return context;
    }
}
