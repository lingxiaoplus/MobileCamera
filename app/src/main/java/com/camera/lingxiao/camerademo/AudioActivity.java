package com.camera.lingxiao.camerademo;

import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.camera.lingxiao.camerademo.crash.ContentValue;
import com.camera.lingxiao.camerademo.utils.FileUtil;
import com.media.lingxiao.harddecoder.EncoderParams;
import com.media.lingxiao.harddecoder.decoder.AudioDecoder;
import com.media.lingxiao.harddecoder.encoder.AudioEncoder;
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
    private static final int PLAY_PCM = 0;
    private static final int DECODE_TO_PCM = 1;

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
                    getFileList(PLAY_PCM);
                }

                break;
            case R.id.button_encode:
                if (AudioEncoder.isEncoding()){
                    AudioEncoder.getInstance().stopEncodeAac();
                    mBtnEncoder.setText("使用mediacodec编码pcm为aac");
                }else {
                    AudioEncoder.getInstance()
                            .setEncoderParams(getAudioParams())
                            .startEncodeAacData(true);
                    mBtnEncoder.setText("停止编码");
                }
                break;
            case R.id.button_decode:
                if (AudioDecoder.isDncoding()){
                    AudioDecoder.getInstance().stopDecode();
                    mBtnEncoder.setText("使用mediacodec解码aac为pcm");
                }else {
                    getFileList(DECODE_TO_PCM);
                }
                break;
        }
    }

    private void getFileList(int type){
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
                        showDialog(files,type);
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

    private void showDialog(final String[] files,int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择播放文件");
        builder.setItems(files, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (type == PLAY_PCM){
                    AudioUtil.getInstance().startPlay(files[i]);
                    mBtnPlay.setText("停止播放");
                    dialogInterface.dismiss();
                }else if (type == DECODE_TO_PCM){
                    EncoderParams params = getAudioParams();
                    params.setAudioPath("pcm_"+System.currentTimeMillis()+".pcm");
                    AudioDecoder.getInstance()
                            .setEncoderParams(getAudioParams())
                            .startDecode(files[i]);
                    mBtnEncoder.setText("停止解码");
                }

            }
        });
        builder.show();
    }

    private EncoderParams getAudioParams(){
        EncoderParams params = new EncoderParams();
        params.setAudioSampleRate(44100);
        params.setAudioBitrate(1024 * 100);
        params.setAudioChannelConfig(AudioFormat.CHANNEL_IN_MONO);
        params.setAudioFormat(AudioFormat.ENCODING_PCM_16BIT);
        params.setAudioSouce(MediaRecorder.AudioSource.MIC);
        params.setAudioPath(ContentValue.MAIN_PATH + "/aac-" + System.currentTimeMillis() + ".aac");
        return params;
    }

}
