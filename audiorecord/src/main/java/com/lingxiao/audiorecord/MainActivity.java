package com.lingxiao.audiorecord;

import android.Manifest;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    private static final String TAG = "MainActivity";
    private Button mBtnAudio,mBtnPlay;
    private String mFileName = "test";
    private String[] perms = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};
    private String mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioSimple/";

    private Handler mHandler = new Handler();
    private Runnable mAudioRunnableTask = new Runnable() {
        @Override
        public void run() {
            boolean result = AudioUtil.getInstance().startAudioRecord(mFileName);
            if (result){
                Log.e(TAG, "开始录音");
            }else {
                Log.e(TAG, "录音失败");
                /*mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"录音失败",Toast.LENGTH_SHORT).show();
                    }
                });*/
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnAudio = findViewById(R.id.button_audio);
        mBtnPlay = findViewById(R.id.button_play);
        mBtnAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AudioUtil.getInstance().isRecording()){
                    AudioUtil.getInstance().stopAudioRecord();
                    mBtnAudio.setText("开始录音");
                }else {
                    mBtnAudio.setText("停止录音");
                    new Thread(mAudioRunnableTask).start();
                }
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AudioUtil.getInstance().isPlaying()){
                    mBtnPlay.setText("停止播放");
                }else {
                    mBtnPlay.setText("开始播放");
                    File file = new File(mPath+"test.pcm");
                    AudioUtil.getInstance().doPlay(file);
                }

            }
        });
        methodRequiresTwoPermission();
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void methodRequiresTwoPermission() {
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    200, perms);
        }
    }
}
