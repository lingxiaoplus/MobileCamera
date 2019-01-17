package com.camera.lingxiao.camerademo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.media.lingxiao.harddecoder.utils.YuvUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CameraUtil {
    private int mOrienta = 0;//时针旋转的角度
    private int mAngle = 0;//需要顺时针旋转的角度
    private Camera mCamera;
    private int mCameraId = 0;   //默认后置摄像头
    private Matrix mMatrix;
    private int mWidth, mHeight;
    private Activity mActivity;
    private String mPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private String mFileName; //照片名字
    private String mFileDir; //照片目录
    private MediaRecorder mediaRecorder;
    private int defaultVideoFrameRate = 30; //视频默认帧率 无法设置
    private boolean mInitCameraResult;  //相机是否初始化成功
    private static final String TAG = CameraUtil.class.getSimpleName();

    public CameraUtil(Camera camera, int cameraId) {
        mCamera = camera;
        mCameraId = cameraId;
    }

    /**
     * 初始化相机
     *
     * @param width
     * @param height
     */
    //private ReentrantLock lock = new ReentrantLock();
    public void initCamera(int width, int height, Activity activity) {
        mActivity = activity;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        //根据设置的宽高 和手机支持的分辨率对比计算出合适的宽高算法
        Camera.Size optionSize = getOptimalPreviewSize(width, height);
        mWidth = optionSize.width;
        mHeight = optionSize.height;
        LogUtil.e("最后得到的分辨率："+"width: "+mWidth+"  height: "+mHeight);
        parameters.setPreviewSize(optionSize.width, optionSize.height);
        //设置照片尺寸
        parameters.setPictureSize(optionSize.width, optionSize.height);
        //设置实时对焦 部分手机不支持会crash
        //parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        mCamera.setParameters(parameters);
        setCameraDisplayOrientation(activity);
        //开启预览
        mCamera.startPreview();

       /* mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                if (null != mPreviewCallback) {
                    byte[] yuvData = bytes;
                    if (mOrienta != 0) {
                        //说明有旋转角度 最好在native层做数据处理
                        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                            yuvData = rotateYUVDegree90(bytes,mWidth,mHeight);
                        }else {
                            yuvData = rotateYUVDegree270AndMirror(bytes,mWidth,mHeight);
                        }
                    }
                    mPreviewCallback.onPreviewFrame(yuvData, camera);
                    mCamera.addCallbackBuffer(bytes);
                }

            }
        });*/
        //1.设置回调:系统相机某些核心部分不走JVM,进行特殊优化，所以效率很高

        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] datas, Camera camera) {
                //if (!lock.tryLock()) return;
                if (null != mPreviewCallback) {
                     byte[] yuvData = datas;
                    if (mOrienta != 0) {
                        //说明有旋转角度 最好在native层做数据处理
                        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                            //long before = System.currentTimeMillis();
                            //yuvData = rotateYUVDegree90(datas,mWidth,mHeight);  //耗时比较久
                            yuvData = YuvUtil.rotateYuv90(datas,mWidth,mHeight); //70ms-120ms之间，一般稳定在70ms
                            //long after = System.currentTimeMillis();
                            //Log.e(TAG, "旋转yuv耗时: "+(after-before)+"ms");
                        }else {
                            yuvData = YuvUtil.rotateYUVDegree270AndMirror(datas,mWidth,mHeight);
                        }
                    }
                    mPreviewCallback.onPreviewFrame(yuvData, camera);
                    //回收缓存处理 必须放这里 不然会出现垂直同步问题
                    camera.addCallbackBuffer(datas);
                }
                //lock.unlock();
            }
        });
        //2.增加缓冲区buffer: 这里指定的是yuv420sp格式
        mCamera.addCallbackBuffer(new byte[((width * height) *
                ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8]);
        mInitCameraResult = true;
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
     *
     * @param w
     * @param h
     * @return 返回一个Camera.Size类型 通过setPreviewSize设置给相机
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
     *
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
                    mCameraId = 1;

                    try {
                        mCamera.setPreviewDisplay(holder);
                        initCamera(mWidth, mHeight, mActivity);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mInitCameraResult = false;
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
                    mCameraId = 0;

                    try {
                        mCamera.setPreviewDisplay(holder);
                        initCamera(mWidth, mHeight, mActivity);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mInitCameraResult = false;
                    }
                    break;
                }
            }
        }
    }

    @SuppressLint("NewApi")
    public boolean initRecorder(String filePath, SurfaceHolder holder) {

        if (!mInitCameraResult) {
            LogUtil.i("相机未初始化成功");
            return false;
        }
        try {
            // TODO init button
            //mCamera.stopPreview();
            mediaRecorder = new MediaRecorder();
            mCamera.unlock();
            mediaRecorder.setCamera(mCamera);
            if (mCameraId == 1) {
                mediaRecorder.setOrientationHint(270);
            } else {
                mediaRecorder.setOrientationHint(90);
            }

            // 这两项需要放在setOutputFormat之前,设置音频和视频的来源
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);//摄录像机
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);//相机

            // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            //这两项需要放在setOutputFormat之后  设置编码器
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            // 设置录制的视频编码h263 h264
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
            mediaRecorder.setVideoSize(mWidth, mHeight);
            // 设置视频的比特率 (清晰度)
            mediaRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);
            // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
            /*if (defaultVideoFrameRate != -1) {
                mediaRecorder.setVideoFrameRate(defaultVideoFrameRate);
            }*/
            // 设置视频文件输出的路径 .mp4
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.setMaxDuration(30000);
            mediaRecorder.setPreviewDisplay(holder.getSurface());
            mediaRecorder.prepare();

            mediaRecorder.start();  //开始
        } catch (Exception e) {
            e.printStackTrace();
            stopPreview();
            return false;
        }
        return true;
    }


    public void stopRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            LogUtil.i("停止录像");
            if (mCamera != null) {
                try {
                    mCamera.reconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private PictureTakenCallBack mPicListener;

    public void setPicTakenListener(PictureTakenCallBack picListener) {
        this.mPicListener = picListener;
    }

    public interface PictureTakenCallBack {
        void onPictureTaken(String result, File file);
    }

    /**
     * 拍照
     *
     * @param fileName 照片名字
     * @param filePath 照片的路径
     */
    public void takePicture(String fileName, String filePath) {
        this.mFileName = fileName;
        this.mFileDir = mPath + "/" + filePath;
        FileUtil.decideDirExist(mFileDir);  //创建文件夹
        //拍照前 自动对焦
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    LogUtil.i("对焦成功");
                } else {
                    LogUtil.i("对焦失败");
                }
            }
        });
        // TODO: 18-6-18 如果想要在相册里看见该图片 需要更新系统图库，这里我就不做处理了 
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File file = null;
                try {
                    if (mPicListener != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                                data.length);
                        //因为照片有可能是旋转的，这里要做一下处理
                        Camera.CameraInfo info = new Camera.CameraInfo();
                        Camera.getCameraInfo(mCameraId, info);
                        Bitmap realBmp = FileUtil.rotaingBitmap(info.orientation, bitmap);

                        file = FileUtil.saveFile(realBmp, mFileName, mFileDir + "/");
                        mPicListener.onPictureTaken("", file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtil.i("错误：  " + e.getMessage());
                    if (mPicListener != null) {
                        mPicListener.onPictureTaken("保存失败：" + e.getMessage(), file);
                    }
                }
                mCamera.startPreview();
            }
        });
    }

    /**
     * 旋转yuv格式的数据270度并镜像翻转
     * @param data
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    private byte[] rotateYUVDegree270AndMirror(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate and mirror the Y luma
        int i = 0;
        int maxY = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            maxY = imageWidth * (imageHeight - 1) + x * 2;
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[maxY - (y * imageWidth + x)];
                i++;
            }
        }
        // Rotate and mirror the U and V color components
        int uvSize = imageWidth * imageHeight;
        i = uvSize;
        int maxUV = 0;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            maxUV = imageWidth * (imageHeight / 2 - 1) + x * 2 + uvSize;
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[maxUV - 2 - (y * imageWidth + x - 1)];
                i++;
                yuv[i] = data[maxUV - (y * imageWidth + x)];
                i++;
            }
        }
        return yuv;
    }

    /**
     * yuv旋转90度  y = width*height，u = y/4 v = y/4   耗时150ms-200ms之间
     * @param data
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    private byte[] rotateYUVDegree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }


    private PreviewCallback mPreviewCallback;

    public void setPreviewCallback(PreviewCallback previewCallback) {
        this.mPreviewCallback = previewCallback;
    }

    public interface PreviewCallback {
        void onPreviewFrame(byte[] data, Camera camera);
    }

    public void stopPreview() {
        if (null != mCamera) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mInitCameraResult = false;
        }
    }

    public int getWidth(){
        if (mOrienta != 0)
            return mHeight;
        return mWidth;
    }
    public int getHeight(){
        if (mOrienta != 0)
            return mWidth;
        return mHeight;
    }
}
