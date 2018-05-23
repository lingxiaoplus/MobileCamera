package com.camera.lingxiao.camerademo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);
        mHolder = mSurfaceView.getHolder();
        methodRequiresTwoPermission();

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
            }catch (Exception e){
                LogUtil.i("摄像头被占用");
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                initCamera(mSurfaceView.getWidth(),mSurfaceView.getHeight());
                mCamera.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            CameraUtil.getInstance(mCamera,mCamerId).stopPreview();
        }
    }

    private void initCamera(int width,int height){
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        //根据设置的宽高 和手机支持的分辨率对比计算出合适的宽高算法
        Camera.Size optionSize = CameraUtil
                .getInstance(mCamera, mCamerId)
                .getOptimalPreviewSize(width, height);
        parameters.setPreviewSize(optionSize.width, optionSize.height);
        //设置照片尺寸
        parameters.setPictureSize(optionSize.width, optionSize.height);
        //设置实时对焦 部分手机不支持会crash
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(parameters);
        CameraUtil.getInstance(mCamera,mCamerId).setCameraDisplayOrientation(this);
        //开启预览
        mCamera.startPreview();
    }
}
