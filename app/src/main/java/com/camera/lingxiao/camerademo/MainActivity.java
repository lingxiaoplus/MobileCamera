package com.camera.lingxiao.camerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private String[] perms = {Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION};
    public static final int RC_CAMERA_AND_LOCATION = 1;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private int mCamerId = 0;
    private Camera mCamera;
    private ImageView localImg,shutterImg,changeImg;
    private CameraUtil mCameraUtil;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);
        localImg = findViewById(R.id.iv_local);
        shutterImg = findViewById(R.id.iv_shutter);
        changeImg = findViewById(R.id.iv_change);
        mHolder = mSurfaceView.getHolder();
        methodRequiresTwoPermission();


        changeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mCameraUtil){
                    mCameraUtil.changeCamera(mHolder);
                }
            }
        });

        localImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        shutterImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        //同意授权
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        //拒绝授权
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).setRationale("请同意相机权限").setTitle("提示").build().show();
        }
    }

    private void methodRequiresTwoPermission() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            if (checkCameraHardware(this)){
                mHolder.addCallback(new SurfaceCallback());
            }else {
                Toast.makeText(this,"没有相机硬件",Toast.LENGTH_SHORT).show();
            }
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    RC_CAMERA_AND_LOCATION, perms);
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera = Camera.open(mCamerId);
                mCameraUtil = new CameraUtil(mCamera,mCamerId);
            }catch (Exception e){
                LogUtil.i("摄像头被占用");
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                mCameraUtil
                        .initCamera(mSurfaceView.getWidth(),
                                mSurfaceView.getHeight(),
                                MainActivity.this);
                mCamera.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (null != mCameraUtil){
                mCameraUtil.stopPreview();
            }
        }
    }
}
