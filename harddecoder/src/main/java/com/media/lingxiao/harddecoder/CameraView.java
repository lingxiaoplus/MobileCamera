package com.media.lingxiao.harddecoder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import com.media.lingxiao.harddecoder.encoder.H264EncoderConsumer;
import com.media.lingxiao.harddecoder.utils.CameraUtil;

import java.io.File;

public class CameraView extends TextureView implements TextureView.SurfaceTextureListener,
        CameraUtil.PictureTakenCallBack,CameraUtil.PreviewCallback {
    private int frameWidth;
    private int frameHeight;
    private int mCameraId;
    private CameraUtil mCameraUtil;
    private static final String TAG = CameraView.class.getSimpleName();
    public CameraView(Context context) {
        this(context,null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 获取用户配置属性
        TypedArray tyArry = context.obtainStyledAttributes(attrs, R.styleable.CameraView);
        frameWidth = tyArry.getInt(R.styleable.CameraView_frame_width, 1280);
        frameHeight = tyArry.getInt(R.styleable.CameraView_frame_height, 720);
        boolean camerBack = tyArry.getBoolean(R.styleable.CameraView_camera_back, true);
        mCameraId = camerBack?Camera.CameraInfo.CAMERA_FACING_BACK:Camera.CameraInfo.CAMERA_FACING_FRONT;
        tyArry.recycle();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        try {
            Camera mCamera = Camera.open(mCameraId);
            mCameraUtil = new CameraUtil(mCamera, mCameraId);
            mCameraUtil.setPicTakenListener(this);
            mCameraUtil.setPreviewCallback(this);
            mCamera.setPreviewTexture(surface);
            mCameraUtil.initCamera(frameWidth, frameHeight, MainActivity.this);
        } catch (Exception e) {
            Log.e(TAG,"摄像头被占用");
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onPictureTaken(String result, File file) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
