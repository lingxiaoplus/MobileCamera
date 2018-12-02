package com.camera.lingxiao.camerademo;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.camera.lingxiao.camerademo.utils.AudioUtil;

public class AudioActivity extends BaseActivity {
    private Button mBtnAudio,mBtnPlay;
    private String mFileName = "test";
    private String mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioSimple/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
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
    }
}
