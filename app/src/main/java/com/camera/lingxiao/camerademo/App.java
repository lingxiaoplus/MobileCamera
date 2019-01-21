package com.camera.lingxiao.camerademo;

import android.app.Application;

import com.camera.lingxiao.camerademo.crash.ContentValue;
import com.camera.lingxiao.camerademo.crash.CrashHandler;

import java.io.File;


public class App extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this,true);
    }
}
