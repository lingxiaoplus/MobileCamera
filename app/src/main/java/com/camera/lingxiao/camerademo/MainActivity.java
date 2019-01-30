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
import com.media.lingxiao.harddecoder.CameraView;
import com.media.lingxiao.harddecoder.EncoderParams;
import com.media.lingxiao.harddecoder.Server;
import com.media.lingxiao.harddecoder.encoder.H264Encoder;
import com.media.lingxiao.harddecoder.model.VideoStreamModel;
import com.media.lingxiao.harddecoder.tlv.ServerConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends BaseActivity {
    @BindView(R.id.surfaceView)
    CameraView mCameraView;
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
    private long mSeq_no0;
    private long mSeq_no1;
    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        new Thread(()-> {
            mServer = new Server();
            boolean ret = mServer.start(2333, new ServerConfig());
            if(ret){
                Log.d(TAG, "服务器启动成功，端口: 2333");
            }
        }).start();

        mCameraView.setPicTakenCallBack(new CameraView.PictureTakenCallBack() {
            @Override
            public void onPictureTaken(String result, File file) {
                if (result.isEmpty()) {
                    localImgUri = Uri.fromFile(file);
                    Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_SHORT).show();
                    localImg.setImageURI(localImgUri);
                } else {
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                }
                shutterImg.setEnabled(true);
            }
        });
        mCameraView.setCameraDataCallback(new CameraView.CameraDataCallback() {
            @Override
            public void onYuvDataFrame(byte[] yuv, Camera camera) {

            }

            @Override
            public void onH264DataFrame(byte[] h264, int width, int height) {
                //硬编码之后的h264数据
                //发送数据
                if (null != mServer) {
                    byte[] h264Data = Arrays.copyOf(h264, h264.length);
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

            @Override
            public void onAacDataFrame(byte[] aac, int length) {

            }
        });

    }

    @OnClick(R.id.iv_change)
    public void changeCamera(View v){
        int rotation = getWindowManager()
                .getDefaultDisplay()
                .getRotation();
        mCameraView.changeCamera();
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
        mCameraView.takePicture(ContentValue.MAIN_PATH + "/Pictures",getSystemTime() + ".jpg");
    }
    @OnClick(R.id.bt_encoder)
    public void enCodeH264(View v){
        //启动线程编码  注意宽高
        if (null == mH264Encoder) {
            EncoderParams params = new EncoderParams();
            params.setVideoPath(ContentValue.MAIN_PATH + "/testYuv.yuv");
            mH264Encoder = new H264Encoder(mCameraView.getFrameWidth(), mCameraView.getFrameHeight(), framerate, biterate, params);
        }

        if (!mH264Encoder.isEncodering()) {
            mH264Encoder.StartEncoderThread();
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
        //启动线程编码  注意宽高  设置监听器
        if (!mCameraView.isRecoder()){
            mCameraView.startHardRecorde(getVideoParams());
            mBtnMuxer.setText("停止混合编码");

        }else {
            mCameraView.stopHardRecorde();
            mBtnMuxer.setText("音视频混合");
        }
    }
    @OnClick(R.id.bt_recode)
    public void onRecode(View v){

        if (mCameraView.isRecoder()){
            mCameraView.stopRecorde();
            recorderBtn.setText("录制");
        }else {
            String filePath = ContentValue.MAIN_PATH + "/mediaRecorder_"
                    + getSystemTime() + ".mp4";
            EncoderParams params = getVideoParams();
            params.setVideoPath(filePath);
            params.setVideoQuality(EncoderParams.MIDDLE_VIDEO_BIT_RATE);
            mCameraView.startRecorde(params);
            Toast.makeText(getApplicationContext(), "文件保存在：" + filePath,
                    Toast.LENGTH_LONG).show();
            recorderBtn.setText("停止录制");
        }
    }

    private EncoderParams getVideoParams(){
        EncoderParams params = new EncoderParams();
        params.setAudioSampleRate(44100);
        params.setAudioBitrate(1024 * 100);
        params.setFrameWidth(mCameraView.getFrameWidth());
        params.setFrameHeight(mCameraView.getFrameHeight());
        params.setFrameRate(framerate);
        params.setVideoQuality(EncoderParams.MIDDLE_VIDEO_BIT_RATE);
        params.setAudioChannelConfig(AudioFormat.CHANNEL_IN_MONO);
        params.setAudioFormat(AudioFormat.ENCODING_PCM_16BIT);
        params.setAudioSouce(MediaRecorder.AudioSource.MIC);
        params.setVideoPath(ContentValue.MAIN_PATH + "/muxer_" + getSystemTime() + ".mp4");
        return params;
    }

    private static String getSystemTime(){
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
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
