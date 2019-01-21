package com.camera.lingxiao.camerademo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camera.lingxiao.camerademo.crash.ContentValue;
import com.camera.lingxiao.camerademo.utils.CameraUtil;
import com.camera.lingxiao.camerademo.utils.LogUtil;
import com.media.lingxiao.harddecoder.EncoderParams;
import com.media.lingxiao.harddecoder.Server;
import com.media.lingxiao.harddecoder.encoder.AudioEncoder;
import com.media.lingxiao.harddecoder.encoder.H264Encoder;
import com.media.lingxiao.harddecoder.encoder.H264EncoderConsumer;
import com.media.lingxiao.harddecoder.model.VideoStreamModel;
import com.media.lingxiao.harddecoder.tlv.ServerConfig;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity implements H264Encoder.PreviewFrameListener {
    @BindView(R.id.surfaceView)
    SurfaceView mSurfaceView;
    @BindView(R.id.bt_recode)
    Button recorderBtn;
    @BindView(R.id.bt_encoder)
    Button mBtnEncoder;
    @BindView(R.id.bt_decoder)
    Button mBtnDecoder;
    @BindView(R.id.bt_muxer)
    Button mBtnMuxer;
    @BindView(R.id.tv_recode_time)
    TextView tvRecodeTime;
    @BindView(R.id.iv_local)
    CircleImageView localImg;
    @BindView(R.id.iv_shutter)
    ImageView shutterImg;
    @BindView(R.id.iv_change)
    ImageView changeImg;

    public static final int RC_CAMERA_AND_LOCATION = 1;
    private SurfaceHolder mHolder;
    private int mCamerId = 0;
    private Camera mCamera;
    private CameraUtil mCameraUtil;
    private Uri localImgUri;
    private int mWidth = 1920;
    private int mHeight = 1080;
    private int framerate = 30;
    private int biterate = 8500 * 1000;
    private H264Encoder mH264Encoder;
    private Server mServer;
    private long timestamp;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        mHolder = mSurfaceView.getHolder();
        if (checkCameraHardware(this)) {
            mHolder.addCallback(new SurfaceCallback());
                /*mHolder.setFixedSize(getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels);*/
        } else {
            Toast.makeText(this, "没有相机硬件", Toast.LENGTH_SHORT).show();
        }

        new Thread(()-> {
            mServer = new Server();
            boolean ret = mServer.start(2333, new ServerConfig());
            if(ret){
                Log.d(TAG, "服务器启动成功，端口: 2333");
            }
        }).start();

    }

    @OnClick(R.id.iv_change)
    public void changeCamera(View v){
        if (null != mCameraUtil) {
            mCameraUtil.changeCamera(mHolder);
        }
    }
    @OnClick(R.id.iv_local)
    public void showLocalImage(View v){
        if (null == localImgUri) {
            return;
        }
        //打开指定的一张照片
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(localImgUri, "image/*");
        startActivity(intent);
    }
    @OnClick(R.id.iv_shutter)
    public void takePicture(View v){
        shutterImg.setEnabled(false);
        if (null != mCameraUtil) {
            mCameraUtil
                    .takePicture(System.currentTimeMillis() + ".jpg",
                            "CameraDemo");
        }
    }
    @OnClick(R.id.bt_encoder)
    public void enCodeH264(View v){
        //启动线程编码  注意宽高
        if (null != mCameraUtil && null == mH264Encoder) {
            EncoderParams params = new EncoderParams();
            params.setVideoPath(ContentValue.MAIN_PATH + "/testYuv.yuv");
            mH264Encoder = new H264Encoder(mCameraUtil.getWidth(), mCameraUtil.getHeight(), framerate, biterate, params);
        }

        if (!mH264Encoder.isEncodering()) {
            mH264Encoder.StartEncoderThread();
            mH264Encoder.setPreviewListner(MainActivity.this);
            mBtnEncoder.setText("停止编码");
        } else {
            mH264Encoder.stopThread();
            mBtnEncoder.setText("编码h264");
        }
    }
    @OnClick(R.id.bt_decoder)
    public void deCodeH264(View v){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    @OnClick(R.id.bt_muxer)
    public void muxerToMp4(View v){
        //启动线程编码  注意宽高
        if (!H264EncoderConsumer.getInstance().isEncodering()) {
            H264EncoderConsumer.getInstance()
                    .setEncoderParams(getVideoParams())
                    .StartEncodeH264Data();
            AudioEncoder.getInstance()
                    .setEncoderParams(getVideoParams())
                    .startEncodeAacData(false);
            mBtnMuxer.setText("停止混合编码");
        } else {
            H264EncoderConsumer.getInstance().stopEncodeH264();
            AudioEncoder.getInstance().stopEncodeAac();
            mBtnMuxer.setText("音视频混合");
        }
    }
    @OnClick(R.id.bt_recode)
    public void onRecode(View v){
        if (null != mCameraUtil) {
            if (isRecorder) {
                //如果是正在录制 就点击停止录制 不往后走了
                mCameraUtil.stopRecorder();
                recorderBtn.setText("录制");
                return;
            }

            String filePath = ContentValue.MAIN_PATH + "/"
                    + System.currentTimeMillis() + ".mp4";
            isRecorder = mCameraUtil.initRecorder(filePath, mHolder);

            if (isRecorder) {
                Toast.makeText(getApplicationContext(), "文件保存在：" + filePath,
                        Toast.LENGTH_LONG).show();
                LogUtil.i("正在录制");
                recorderBtn.setText("停止录制");
            }
        }
    }
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private EncoderParams getVideoParams(){
        EncoderParams params = new EncoderParams();
        params.setAudioSampleRate(44100);
        params.setAudioBitrate(1024 * 100);
        params.setFrameWidth(mCameraUtil.getWidth());
        params.setFrameHeight(mCameraUtil.getHeight());
        params.setFrameRate(framerate);
        params.setVideoQuality(EncoderParams.MIDDLE_VIDEO_BIT_RATE);
        params.setAudioChannelConfig(AudioFormat.CHANNEL_IN_MONO);
        params.setAudioFormat(AudioFormat.ENCODING_PCM_16BIT);
        params.setAudioSouce(MediaRecorder.AudioSource.MIC);
        params.setVideoPath(ContentValue.MAIN_PATH + "/muxer-" + System.currentTimeMillis() + ".mp4");
        return params;
    }

    private boolean isRecorder;
    private long mSeq_no0;
    private long mSeq_no1;

    //h264回调
    @Override
    public void onPreview(byte[] data, int width, int height) {
        //硬编码之后的h264数据
        //发送数据
        if (null != mServer) {
            byte[] h264Data = Arrays.copyOf(data, data.length);
            VideoStreamModel model = new VideoStreamModel();
            mSeq_no0++;
            mSeq_no1++;
            model.setType(2);
            model.setWidth(width);
            model.setHeight(height);
            model.setSeq_no0(mSeq_no0);
            model.setSeq_no1(mSeq_no1);
            model.setVideo(h264Data);
            mServer.broadcastPreviewFrameData(model);
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            try {
                mCamera = Camera.open(mCamerId);
                mCameraUtil = new CameraUtil(mCamera, mCamerId);
                mCameraUtil.setPicTakenListener((result,file)-> {
                    if (result.isEmpty()) {
                        localImgUri = Uri.fromFile(file);
                        Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_SHORT).show();
                        localImg.setImageURI(localImgUri);
                    } else {
                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                    }
                    shutterImg.setEnabled(true);
                });
                mCameraUtil.setPreviewCallback((data,camera)-> {
                    if (null != mH264Encoder) {
                        //给队列丢数据
                        mH264Encoder.putYUVData(data);
                    }
                    H264EncoderConsumer.getInstance().addYUVData(data);
                });
            } catch (Exception e) {
                LogUtil.i("摄像头被占用");
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                mCameraUtil
                        .initCamera(mWidth,
                                mHeight,
                                MainActivity.this);
                mCamera.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (null != mCameraUtil) {
                mCameraUtil.stopRecorder();
                mCameraUtil.stopPreview();
            }
            if (null != mH264Encoder) {
                mH264Encoder.stopThread();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = data.getData();
                String path = uri.getPath();
                Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), PlayActivity.class);
                intent.putExtra("path", path);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServer != null) {
            mServer.stop();
        }
    }
}
