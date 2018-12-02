package com.camera.lingxiao.camerademo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.camera.lingxiao.camerademo.crash.AppManager;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getAppManager().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getAppManager().finishActivity(this);
    }
}
