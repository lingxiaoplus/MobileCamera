package com.camera.lingxiao.camerademo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.camera.lingxiao.camerademo.crash.ContentValue;
import com.camera.lingxiao.camerademo.utils.FileUtil;
import com.media.lingxiao.harddecoder.utils.AudioUtil;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AudioActivity extends BaseActivity {
    @BindView(R.id.button_audio)
    Button mBtnAudio;
    @BindView(R.id.button_play)
    Button mBtnPlay;
    @BindView(R.id.button_encode)
    Button mBtnEncoder;
    @BindView(R.id.button_decode)
    Button mBtnDecoder;

    private String mFileName = "test";
    private String mPath = ContentValue.MAIN_PATH + "/AudioSimple/";
    private static final String TAG = AudioActivity.class.getSimpleName();

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_audio;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("音频采集");
        }

        AudioUtil.getInstance().setAudioListener(new AudioUtil.AudioListener() {
            @Override
            public void onRecordFinish() {
                mBtnAudio.setText("使用AudioTrack录音");
            }

            @Override
            public void onPlayFinish() {
                mBtnPlay.setText("使用AudioTrack播放");
            }

            @Override
            public void onError(String errMsg) {
                Log.e(TAG, "异常 onError: "+errMsg);
            }
        });
    }


    @OnClick({R.id.button_audio,R.id.button_play,R.id.button_encode,R.id.button_decode})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.button_audio:
                if (AudioUtil.getInstance().isRecording()) {
                    AudioUtil.getInstance().stopAudioRecord();
                } else {
                    mBtnAudio.setText("停止录音");
                    AudioUtil.getInstance().startAudioRecord(mFileName);
                }
                break;
            case R.id.button_play:
                if (AudioUtil.getInstance().isPlaying()) {
                    AudioUtil.getInstance().stopPlay();

                } else {
                    showProgressDialog();
                    Observable.create(new ObservableOnSubscribe<String[]>() {
                        @Override
                        public void subscribe(ObservableEmitter<String[]> emitter) {
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

                break;
            case R.id.button_encode:
                break;
            case R.id.button_decode:
                break;
        }
    }
    private void showDialog(final String[] files) {
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
