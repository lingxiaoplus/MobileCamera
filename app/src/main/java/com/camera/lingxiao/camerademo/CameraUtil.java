package com.camera.lingxiao.camerademo;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.lang.ref.Reference;
import java.util.List;

public class CameraUtil {
    private  int mOrienta = 0;//时针旋转的角度
    private  int mAngle = 0;//需要顺时针旋转的角度
    private  Camera mCamera;
    private  int mCameraId = 0;   //默认后置摄像头
    private Matrix mMatrix;
    private int mWidth,mHeight;
    private Activity mActivity;
    public CameraUtil(Camera camera,int cameraId){
        mCamera = camera;
        mCameraId = cameraId;
    }

    /**
     * 初始化相机
     * @param width
     * @param height
     */
    public void initCamera(int width,int height,Activity activity){
        this.mWidth = width;
        this.mHeight = height;
        this.mActivity = activity;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        //根据设置的宽高 和手机支持的分辨率对比计算出合适的宽高算法
        Camera.Size optionSize = getOptimalPreviewSize(width, height);
        parameters.setPreviewSize(optionSize.width, optionSize.height);
        //设置照片尺寸
        parameters.setPictureSize(optionSize.width, optionSize.height);
        //设置实时对焦 部分手机不支持会crash
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(parameters);
        setCameraDisplayOrientation(activity);
        //开启预览
        mCamera.startPreview();

        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

            }
        });
    }

    /**
     * 得到摄像头默认旋转角度后，旋转回来  注意是逆时针旋转
     *
     * @param activity
     */
    private void setCameraDisplayOrientation(Activity activity) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        LogUtil.i("摄像头被旋转的角度;" + result);
        mOrienta = result;//该值有其它用途
        mCamera.setDisplayOrientation(result);
    }
    /**
     * 相机设置旋转后，预览图片和相机返回实时流角度
     * 这个是顺时针旋转
     */
    public void rotateYUVDegree() {
        //mOrienta来源于setCameraDisplayOrientation
        mMatrix = new Matrix();
        switch (mOrienta) {
            case 90:
                mAngle = 270;
                mMatrix.postRotate(270);
                break;
            case 270:
                mAngle = 90;
                mMatrix.postRotate(90);
                break;
            default:
                mAngle = mOrienta;
                mMatrix.postRotate(mOrienta);
                break;
        }
    }
    /**
     * 通过传入的宽高  计算出最接近相机支持的宽高
     * @param w
     * @param h
     * @return  返回一个Camera.Size类型 通过setPreviewSize设置给相机
     */
    public Camera.Size getOptimalPreviewSize(int w, int h) {

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            Log.i("Main", "width: " + size.width + "  height：" + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * 切换前后摄像头
     * @param holder
     */
    public void changeCamera(SurfaceHolder holder) {
        //切换前后摄像头
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (mCameraId == 0) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    /*try {
                        camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setCameraDisplayOrientation(this, i, camera);
                    camera.startPreview();//开始预览*/
                    mCameraId = 1;
                    try {
                        mCamera.setPreviewDisplay(holder);
                        initCamera(mWidth,mHeight,mActivity);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    /*try {
                        camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    setCameraDisplayOrientation(this, i, camera);
                    camera.startPreview();//开始预览*/
                    mCameraId = 0;
                    try {
                        mCamera.setPreviewDisplay(holder);
                        initCamera(mWidth,mHeight,mActivity);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    public void stopPreview(){
        if (null != mCamera) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
