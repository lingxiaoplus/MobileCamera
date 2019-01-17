package com.camera.lingxiao.camerademo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.camera.lingxiao.camerademo.crash.ContentValue;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;
import pub.devrel.easypermissions.EasyPermissions;

public class SplashActivity extends BaseActivity{

    @BindView(R.id.button_extractor)
    Button mButtonExtractor;
    @BindView(R.id.button_camera)
    Button mButtonCamera;
    @BindView(R.id.button_audio)
    Button mButtonAudio;

    public static final int RC_CAMERA_AND_LOCATION = 1;

    private String[] perms = {Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        methodRequiresTwoPermission();
    }

    @OnClick({R.id.button_extractor,R.id.button_camera,R.id.button_audio})
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.button_extractor:
                startActivity(new Intent(getApplicationContext(), MediaExtractActivity.class));
                break;
            case R.id.button_camera:
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;
            case R.id.button_audio:
                startActivity(new Intent(getApplicationContext(), AudioActivity.class));
                break;
        }
    }

    private void methodRequiresTwoPermission() {
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    RC_CAMERA_AND_LOCATION, perms);
        } else {
            createPath();
        }
    }

    private void createPath() {
        File rootDir = new File(ContentValue.MAIN_PATH);
        if (rootDir.exists()) {
            if (rootDir.isFile()) {
                rootDir.delete();
                rootDir.mkdirs();
            }
        } else {
            rootDir.mkdirs();
        }
    }

}
