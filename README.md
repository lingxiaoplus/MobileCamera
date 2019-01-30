# MobileCamera

camera入门系列，基于camera，后期换为camera2实现。主要实现的功能有：

1. 相机预览画面
2. 使用Mediacodec编码pcm格式数据为aac
3. 使用Mediacodec解码aac格式数据为pcm
4. 使用Mediacodec编码yuv格式数据为h264
5. 使用MediaRecorder录制视频
6. 使用Mediacodec+AudioRecord获取原始数据，使用MediaMuxer混合音视频数据并封装为mp4格式文件。

####使用方法：

使用之前添加权限：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

在布局文件中加入：

```xml
<com.media.lingxiao.harddecoder.widget.CameraView
        android:id="@+id/mCameraView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:frame_width="1920"
        app:frame_height="1080"
        app:camera_back="true"
        />
```

切换相机：

```java
CameraView mCameraView = findViewById(R.id.mCameraView);
mCameraView.changeCamera();
```

拍照：

```java
mCameraView.takePicture(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CameraPictures","demo.jpg");
mCameraView.setPicTakenListener(new CameraView.PictureTakenCallBack() {
            @Override
            public void onPictureTaken(String result, File file) {
                if (result.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                } 
            }
        });
```

使用MediaRecorder录像：

```java
if (mCameraView.isRecoder()){//先判断是否正在录制
            mCameraView.stopRecorde();
        }else {
            String filePath = ContentValue.MAIN_PATH + "/mediaRecorder_"
                    + getSystemTime() + ".mp4";
            mCameraView.startRecorde(filePath);
            Toast.makeText(getApplicationContext(), "文件保存在：" + filePath,
                    Toast.LENGTH_LONG).show();
        }
```

使用Mediacodec+AudioRecord混合录制，在录制之前先配置好参数：

```java
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

if (!mCameraView.isRecoder()){
    mCameraView.startHardRecorde(getVideoParams());
    mBtnMuxer.setText("停止混合编码");
}else {
    mCameraView.stopHardRecorde();
    mBtnMuxer.setText("音视频混合");
}
```

