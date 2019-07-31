package com.media.lingxiao.harddecoder.widget;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.PermissionChecker;

import com.media.lingxiao.harddecoder.EncoderParams;
import com.media.lingxiao.harddecoder.R;
import com.media.lingxiao.harddecoder.encoder.AudioEncoder;
import com.media.lingxiao.harddecoder.encoder.H264EncoderConsumer;
import com.media.lingxiao.harddecoder.utils.FileUtil;
import com.media.lingxiao.harddecoder.utils.YuvUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2View extends TextureView implements TextureView.SurfaceTextureListener,
        H264EncoderConsumer.H264FrameListener,
        AudioEncoder.AudioEncodeListener {
    private int frameWidth;
    private int frameHeight;
    private int mCameraId;   //摄像头id

    private int mOrienta = 0;//时针旋转的角度
    private int mAngle = 0;//需要顺时针旋转的角度
    private Matrix mMatrix;
    private String mFileName; //照片名字
    private String mFileDir; //照片目录
    private MediaRecorder mediaRecorder;
    private int defaultVideoFrameRate = 30; //视频默认帧率 无法设置
    private Context mContext;
    private SurfaceTexture mTexture;

    private static final String TAG = Camera2View.class.getSimpleName();
    private boolean isRecoder = false;  //是否正在录制
    private CameraHandlerThread mCameraHandlerThread;
    private final Object mCaptureSync = new Object();
    private boolean mCaptureStillImage;
    private Bitmap mCaptureBitmap;

    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharcter;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mRequestBuilder;
    private Surface mSurface;

    public Camera2View(Context context) {
        this(context, null);
    }

    public Camera2View(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Camera2View(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 获取用户配置属性
        TypedArray tyArry = context.obtainStyledAttributes(attrs, R.styleable.CameraView);
        frameWidth = tyArry.getInt(R.styleable.CameraView_frame_width, 1280);
        frameHeight = tyArry.getInt(R.styleable.CameraView_frame_height, 720);
        boolean camerBack = tyArry.getBoolean(R.styleable.CameraView_camera_back, true);
        mCameraId = camerBack ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        tyArry.recycle();
        this.mContext = context;
        if (checkCameraHardware(context)) {
            setSurfaceTextureListener(this);
        } else {
            throw new NullPointerException("未检测到相机硬件");
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            this.mTexture = surface;
            int rotation = getActivityFromContext(mContext)
                    .getWindowManager()
                    .getDefaultDisplay()
                    .getRotation();
            mCameraHandlerThread = new CameraHandlerThread("camera thread");
            mCameraHandlerThread.openCameraByHandler(frameWidth, frameHeight, surface, rotation);
            ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
            float scale = (float) frameHeight / frameWidth;
            layoutParams.width = width;
            float scaleHeight = width * 1.0f / scale;
            layoutParams.height = (int) scaleHeight;
            setLayoutParams(layoutParams);
        } catch (Exception e) {
            Log.e(TAG, "摄像头被占用");
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopPreview();
        stopRecorde();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        /*long before = System.currentTimeMillis();
        Bitmap bitmap = getBitmap();
        Log.d(TAG, "获取bitmap耗时："+(System.currentTimeMillis()-before));*/
        /*synchronized (mCaptureSync) {
            if (mCaptureStillImage) {
                long startTime = System.currentTimeMillis();
                mCaptureStillImage = false;
                if (mCaptureBitmap == null)
                    mCaptureBitmap = getBitmap();
                else
                    getBitmap(mCaptureBitmap);
                mCaptureSync.notifyAll();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "耗时:" + (endTime - startTime));
            }
        }*/
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(frameWidth, frameHeight);
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(frameWidth, heightSpecSize);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, frameHeight);
        }
    }

    @Override
    public void onGetH264(byte[] data, int width, int height) {
        if (null != mCameraDataCallback) {
            mCameraDataCallback.onH264DataFrame(data, width, height);
        }
    }

    @Override
    public void onGetAac(byte[] data, int length) {
        if (null != mCameraDataCallback) {
            mCameraDataCallback.onAacDataFrame(data, length);
        }
    }


    @Override
    public void onStopEncodeH264Success() {

    }

    @Override
    public void onStopEncodeAacSuccess() {

    }

    public Bitmap getCaptureBitmap(int width, int height) {
        synchronized (mCaptureSync) {
            mCaptureStillImage = true;
            try {
                mCaptureSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            return mCaptureBitmap;
        }
    }


    private class CameraHandlerThread extends HandlerThread {
        private Handler mHandler;

        public CameraHandlerThread(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }

        protected synchronized void notifyCameraOpen() {
            notify();
        }

        public synchronized void openCameraByHandler(final int width, final int height, final SurfaceTexture texture, final int rotation) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //post()执行会立即返回，而Runnable()会异步执行，可能在执行post()后立即使用mCamera时仍为null 所以用notify-wait
                    Camera2View.this.openCamera(width, height, texture, rotation);
                    notifyCameraOpen();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreviewSession();
            Log.d(TAG,"相机打开");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            Log.d(TAG,"相机关闭");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            Log.d(TAG,"相机发生错误，错误码 >> "+error);
        }
    };

    /**
     * 初始化相机
     *
     * @param width
     * @param height
     */
    //private ReentrantLock lock = new ReentrantLock();
    private void openCamera(int width, int height, SurfaceTexture texture, int rotation) {
        try {
            mCameraManager = (CameraManager) getActivityFromContext(mContext).getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIds = mCameraManager.getCameraIdList();
            if (mCameraId > cameraIds.length || mCameraId < 0) mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            String id = cameraIds[mCameraId];
            mCameraCharcter = mCameraManager.getCameraCharacteristics(id);
            Size size = getOptimalPreviewSize(width,height);
            texture.setDefaultBufferSize(size.getWidth(),size.getHeight());
            if (PermissionChecker.checkSelfPermission(mContext,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalArgumentException("没有camer的权限！！");
            }
            mCameraManager.openCamera(id, mStateCallback, mCameraHandlerThread.mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "摄像头被占用");
            e.printStackTrace();
        }
    }


    private void createCameraPreviewSession(){
        SurfaceTexture texture = getSurfaceTexture();
        try {
            // 创建作为预览的CaptureRequest.Builder
            mRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将textureView的surface作为CaptureRequest.Builder的目标
            mSurface = new Surface(texture);
            mRequestBuilder.addTarget(mSurface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    // 设置自动对焦模式
                    mRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    // 设置自动曝光模式
                    mRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    // 开始显示相机预览
                    CaptureRequest captureRequest = mRequestBuilder.build();
                    // 设置预览时连续捕获图像数据
                    try {
                        session.setRepeatingRequest(captureRequest,
                                null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG,"相机配置失败");
                }
            },mCameraHandlerThread.mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * onPreviewFrame()在执行Camera.open()时所在的线程运行 onPreviewFrame中有耗时操作，会造成ui卡顿
     */
    private void setCameraCallback() {

    }


    /**
     * 得到摄像头默认旋转角度后，旋转回来  注意是逆时针旋转
     *
     * @param rotation
     */
    private void setCameraDisplayOrientation(int rotation) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        //int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
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
        Log.d(TAG, "摄像头被旋转的角度;" + result);
        mOrienta = result;//该值有其它用途
        //mCamera.setDisplayOrientation(result);
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


    private Activity getActivityFromContext(Context context) {
        if (null != context) {
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    return (Activity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
        }
        return null;
    }

    /**
     * 通过传入的宽高  计算出最接近相机支持的宽高
     *
     * @param w
     * @param h
     * @return 返回一个Camera.Size类型 通过setPreviewSize设置给相机
     */
    private Size getOptimalPreviewSize(int w, int h) {
        StreamConfigurationMap map =
                mCameraCharcter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            Log.i("Main", "width: " + size.getWidth() + "  height：" + size.getHeight());
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * 切换前后摄像头
     */
    public void changeCamera() {
        //切换前后摄像头
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
        int rotation = getActivityFromContext(mContext)
                .getWindowManager()
                .getDefaultDisplay()
                .getRotation();

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (mCameraId == 0) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置

                    //mCamera = Camera.open(i);//打开当前选中的摄像头
                    mCameraId = 1;
                    if (mCameraHandlerThread != null) {
                        mCameraHandlerThread.openCameraByHandler(frameWidth, frameHeight, mTexture, rotation);
                    }
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置

                    //mCamera = Camera.open(i);//打开当前选中的摄像头
                    mCameraId = 0;
                    if (mCameraHandlerThread != null) {
                        mCameraHandlerThread.openCameraByHandler(frameWidth, frameHeight, mTexture, rotation);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 和下面方法的区别是：这个可以对视频数据进行操作，比如添加水印，滤镜等等
     */
    public void startHardRecorde(EncoderParams params) {
        if (!H264EncoderConsumer.getInstance().isEncodering()) {
            H264EncoderConsumer.getInstance()
                    .setEncoderParams(params)
                    .StartEncodeH264Data();
            AudioEncoder.getInstance()
                    .setEncoderParams(params)
                    .startEncodeAacData();
            H264EncoderConsumer.getInstance().setEncodeH264Listner(this);
            this.isRecoder = true;
        }
    }

    public void stopHardRecorde() {
        H264EncoderConsumer.getInstance().stopEncodeH264();
        AudioEncoder.getInstance().stopEncodeAac();
        this.isRecoder = false;
    }

    public boolean startRecorde(EncoderParams params) {
        try {
            // TODO init button
            //mCamera.stopPreview();
            mediaRecorder = new MediaRecorder();
            //mCamera.unlock();
            //mediaRecorder.setCamera(mCamera);
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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
            mediaRecorder.setVideoSize(this.frameWidth, this.frameHeight);
            // 设置视频的比特率 (清晰度)
            mediaRecorder.setVideoEncodingBitRate(params.getVideoQuality() * this.frameWidth * this.frameHeight);
            // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
            /*if (defaultVideoFrameRate != -1) {
                mediaRecorder.setVideoFrameRate(defaultVideoFrameRate);
            }*/
            // 设置视频文件输出的路径 .mp4
            mediaRecorder.setOutputFile(params.getVideoPath());
            mediaRecorder.setMaxDuration(30000);
            //mediaRecorder.setPreviewDisplay(holder.getSurface());

            mediaRecorder.prepare();

            mediaRecorder.start();  //开始
            this.isRecoder = true;
        } catch (Exception e) {
            e.printStackTrace();
            stopPreview();
            return false;
        }
        return true;
    }

    public void stopRecorde() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Log.d(TAG, "停止录像");
            this.isRecoder = false;
            /*if (mCamera != null) {
                try {
                    mCamera.reconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }


    /**
     * 拍照
     *
     * @param fileName 照片名字
     * @param filePath 照片的路径
     */
    public void takePicture(String filePath, String fileName) {
        this.mFileName = fileName;
        this.mFileDir = filePath;
        FileUtil.decideDirExist(mFileDir);  //创建文件夹
        //拍照前 自动对焦
        /*mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    Log.d(TAG, "对焦成功");
                } else {
                    Log.d(TAG, "对焦失败");
                }
            }
        });*/
        // TODO: 18-6-18 如果想要在相册里看见该图片 需要更新系统图库，这里我就不做处理了

        /*mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                Camera2View.this.post(new Runnable() {
                    @Override
                    public void run() {
                        File file = null;
                        try {
                            if (mPicListener != null) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                                        data.length);
                                //因为照片有可能是旋转的，这里要做一下处理
                                Camera.CameraInfo info = new Camera.CameraInfo();
                                Camera.getCameraInfo(mCameraId, info);

                                Bitmap realBmp = FileUtil.rotaingBitmap(info.orientation,
                                        mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT, bitmap);
                                file = FileUtil.saveFile(realBmp, mFileDir + "/" + mFileName);
                                mPicListener.onPictureTaken("", file);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "错误：  " + e.getMessage());
                            if (mPicListener != null) {
                                mPicListener.onPictureTaken("保存失败：" + e.getMessage(), file);
                            }
                        }
                        mCamera.startPreview();
                    }
                });

            }
        });*/
    }

    private PictureTakenCallBack mPicListener;

    public void setPicTakenCallBack(PictureTakenCallBack picListener) {
        this.mPicListener = picListener;
    }

    public interface PictureTakenCallBack {
        void onPictureTaken(String result, File file);
    }

    private CameraDataCallback mCameraDataCallback;

    public void setCameraDataCallback(CameraDataCallback cameraCallback) {
        this.mCameraDataCallback = cameraCallback;
    }

    public interface CameraDataCallback {
        void onYuvDataFrame(byte[] yuv, Camera camera);

        void onH264DataFrame(byte[] h264, int width, int height);

        void onAacDataFrame(byte[] aac, int length);
    }

    public void stopPreview() {
        if (null != mCameraCaptureSession) {
            mCameraCaptureSession.close();
        }
        if (mRequestBuilder != null && mSurface != null){
            mRequestBuilder.removeTarget(mSurface);
        }
    }

    public int getFrameWidth() {
        if (mOrienta != 0)
            return frameHeight;
        return frameWidth;
    }

    public int getFrameHeight() {
        if (mOrienta != 0)
            return frameWidth;
        return frameHeight;
    }

    public boolean isRecoder() {
        return isRecoder;
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }
}
