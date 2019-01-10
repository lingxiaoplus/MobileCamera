package com.camera.lingxiao.camerademo;

import android.content.DialogInterface;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.camera.lingxiao.camerademo.utils.AudioUtil;
import com.camera.lingxiao.camerademo.utils.FileUtil;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AudioActivity extends BaseActivity {
    private Button mBtnAudio,mBtnPlay;
    private Button mBtnEncoder,mBtnDecoder;
    private String mFileName = "test";
    private String mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioSimple/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        mBtnAudio = findViewById(R.id.button_audio);
        mBtnPlay = findViewById(R.id.button_play);
        mBtnEncoder = findViewById(R.id.button_encode);
        mBtnDecoder = findViewById(R.id.bt_decoder);
        mBtnAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AudioUtil.getInstance().isRecording()){
                    AudioUtil.getInstance().stopAudioRecord();
                    mBtnAudio.setText("使用AudioTrack录音");
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
                    mBtnPlay.setText("使用AudioTrack播放");
                }else {
                    showProgressDialog();
                    Observable.create(new ObservableOnSubscribe<String[]>() {
                        @Override
                        public void subscribe(ObservableEmitter<String[]> emitter){
                            List<String> pcmList = FileUtil.getFileList(mPath);
                            String[] files = pcmList.toArray(new String[pcmList.size()]);
                            emitter.onNext(files);
                            emitter.onComplete();

                        }
                    }).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<String[]>() {
                                private Disposable mDisposable;
                                @Override
                                public void onSubscribe(Disposable d) {
                                    mDisposable = d;
                                }

                                @Override
                                public void onNext(String[] files) {
                                    cancelProgressDialog();
                                    showDialog(files);
                                    mDisposable.dispose();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    e.printStackTrace();
                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                }

            }
        });

        mBtnEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    private void showDialog(final String[] files){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择播放文件");
        builder.setItems(files, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AudioUtil.getInstance().startPlay(files[i]);
                mBtnPlay.setText("停止播放");
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }
}
