package com.camera.lingxiao.camerademo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.camera.lingxiao.camerademo.crash.ContentValue;
import com.camera.lingxiao.camerademo.utils.FileUtil;
import com.media.lingxiao.harddecoder.EncoderParams;
import com.media.lingxiao.harddecoder.decoder.AudioDecoder;
import com.media.lingxiao.harddecoder.utils.AudioUtil;

import java.io.File;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.EasyPermissions;

public class SplashActivity extends BaseActivity{

    @BindView(R.id.button_extractor)
    Button mButtonExtractor;
    @BindView(R.id.button_camera)
    Button mButtonCamera;
    @BindView(R.id.button_audio)
    Button mButtonAudio;

    public static final int RC_CAMERA_AND_LOCATION = 1;

    private String[] perms = {Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        methodRequiresTwoPermission();
    }

    @OnClick({R.id.button_extractor,R.id.button_camera,R.id.button_audio})
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.button_extractor:
                startActivity(new Intent(getApplicationContext(), MediaExtractActivity.class));
                break;
            case R.id.button_camera:
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;
            case R.id.button_audio:
                startActivity(new Intent(getApplicationContext(), AudioActivity.class));
                break;
        }
    }

    @OnClick(R.id.button_player)
    public void onGetPlayList(View v){
        showProgressDialog();
        Observable.create(new ObservableOnSubscribe<String[]>() {
            @Override
            public void subscribe(ObservableEmitter<String[]> emitter) {
                List<String> pcmList = FileUtil.getFileList(ContentValue.MAIN_PATH);
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

    private void showDialog(final String[] files) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择播放文件");
        builder.setItems(files, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                /*AudioDecoder.getInstance()
                        .startDecodeFromMPEG_4(files[i]);*/
                Intent intent = new Intent(getApplicationContext(),PlayActivity.class);
                intent.putExtra("path",files[i]);
                startActivity(intent);
            }
        });
        builder.show();
    }
    private void methodRequiresTwoPermission() {
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    RC_CAMERA_AND_LOCATION, perms);
        } else {
            createPath();
        }
    }

    private void createPath() {
        File rootDir = new File(ContentValue.MAIN_PATH);
        if (rootDir.exists()) {
            if (rootDir.isFile()) {
                rootDir.delete();
                rootDir.mkdirs();
            }
        } else {
            rootDir.mkdirs();
        }
    }

}
