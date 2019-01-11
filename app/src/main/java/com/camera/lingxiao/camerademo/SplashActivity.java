package com.camera.lingxiao.camerademo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.camera.lingxiao.camerademo.crash.ContentValue;

import java.io.File;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class SplashActivity extends BaseActivity implements View.OnClickListener,EasyPermissions.PermissionCallbacks{

    /**
     * MediaExtractor视频换音
     */
    private Button mButtonExtractor;
    public static final int RC_CAMERA_AND_LOCATION = 1;
    /**
     * camera采集
     */
    private Button mButtonCamera,mButtonAudio;
    private String[] perms = {Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initView();
        methodRequiresTwoPermission();
    }

    private void initView() {
        mButtonExtractor = (Button) findViewById(R.id.button_extractor);
        mButtonExtractor.setOnClickListener(this);
        mButtonCamera = (Button) findViewById(R.id.button_camera);
        mButtonCamera.setOnClickListener(this);
        mButtonAudio = findViewById(R.id.button_audio);
        mButtonAudio.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.button_extractor:
                startActivity(new Intent(getApplicationContext(),MediaExtractActivity.class));
                break;
            case R.id.button_camera:
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                break;
            case R.id.button_audio:
                startActivity(new Intent(getApplicationContext(),AudioActivity.class));
                break;
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        createPath();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        //拒绝授权
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).setRationale("请同意相机权限").setTitle("提示").build().show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void methodRequiresTwoPermission() {
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    RC_CAMERA_AND_LOCATION, perms);
        }else {
            createPath();
        }
    }

    private void createPath() {
        File rootDir = new File(ContentValue.MAIN_PATH);
        if (rootDir.exists()){
            if (rootDir.isFile()){
                rootDir.delete();
                rootDir.mkdirs();
            }
        }else {
            rootDir.mkdirs();
        }
    }
}
