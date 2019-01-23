package com.media.lingxiao.harddecoder.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CameraUtil {

    private static final String TAG = CameraUtil.class.getSimpleName();
    private static CameraUtil mCameraUtil;
    private SurfaceTexture mTexture;

    private CameraUtil() {

    }

    /*public static CameraUtil open(){
        mCameraUtil
        return mCameraUtil;
    }*/
    public static CameraUtil create(){
        if (mCameraUtil == null){
            mCameraUtil = new CameraUtil();
        }
        return mCameraUtil;
    }

    public CameraUtil SetSurfaceTexture(SurfaceTexture surfaceTexture){
        this.mTexture = surfaceTexture;
        return mCameraUtil;
    }





}
