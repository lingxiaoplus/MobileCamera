package com.camera.lingxiao.camerademo;

import android.app.Application;

import com.camera.lingxiao.camerademo.crash.CrashHandler;
import com.tencent.bugly.crashreport.CrashReport;

public class App extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        //CrashHandler.getInstance().init(this,true);
    }
}
