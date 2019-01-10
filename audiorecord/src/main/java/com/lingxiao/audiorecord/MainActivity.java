package com.lingxiao.audiorecord;

import android.Manifest;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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
                    AudioUtil.getInstance().startRecord(mFileName);
                }
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AudioUtil.getInstance().isPlaying()){
                    AudioUtil.getInstance().stopPlay();
                    mBtnPlay.setText("开始播放");
                }else {
                    mBtnPlay.setText("停止播放");
                    AudioUtil.getInstance().startPlay(mPath+"test.pcm");
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
