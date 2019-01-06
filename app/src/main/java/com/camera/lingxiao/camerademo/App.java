package com.camera.lingxiao.camerademo;

import android.app.Application;


public class App extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        //CrashHandler.getInstance().init(this,true);
    }
}
