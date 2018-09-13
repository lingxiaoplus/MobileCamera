package com.camera.lingxiao.camerademo;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;

public class App extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "be9c7dde9e", true);
    }
}
