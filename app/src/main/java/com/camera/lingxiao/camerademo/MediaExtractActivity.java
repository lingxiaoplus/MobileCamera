package com.camera.lingxiao.camerademo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.camera.lingxiao.camerademo.crash.ContentValue;
import com.camera.lingxiao.camerademo.utils.MediaUtil;

import java.io.File;
import java.io.IOException;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MediaExtractActivity extends BaseActivity {
    private static final int FILE_SELECT_AUDIO = 0;
    private static final int FILE_SELECT_VIDEO = 1;
    private static final String TAG = "MediaExtractActivity";
    @BindView(R.id.button_audio)
    Button mButtonAudio;
    @BindView(R.id.button_video)
    Button mButtonVideo;
    @BindView(R.id.button_start)
    Button mButtonStart;

    private String mComFile = ContentValue.MAIN_PATH + "/comp.mp4";
    private String mAudioPath, mVideoPath;

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_media_extract;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("音视频合成");
        }
    }

    private void showSelect(final int type) {
        String[] items = {"输入相对路径", "文件管理器选择"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MediaExtractActivity.this);
        builder.setTitle("选择文件路径获取方式");
        builder.setItems(items, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            if (i == 0) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(MediaExtractActivity.this);
                final EditText editText = new EditText(MediaExtractActivity.this);
                builder1.setTitle("填写sd卡的文件路径如(/simple/x.mp4)");
                builder1.setView(editText);
                builder1.setPositiveButton("确定", (dialog, which) -> {
                    String dir = editText.getText().toString().trim();
                    String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + dir;
                    File file = new File(filePath);
                    if (!file.exists()) {
                        Toast.makeText(getApplicationContext(), "文件不存在: " + filePath, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (type == FILE_SELECT_AUDIO) {
                        mAudioPath = filePath;
                    } else {
                        mVideoPath = filePath;
                    }
                    dialog.dismiss();
                });
                builder1.show();
            } else {
                if (type == FILE_SELECT_AUDIO) {
                    showFileChooser("选择一个音频文件", FILE_SELECT_AUDIO);
                } else {
                    showFileChooser("选择一个视频文件", FILE_SELECT_VIDEO);
                }
            }

        });
        builder.show();
    }

    @OnClick({R.id.button_audio,R.id.button_video,R.id.button_start})
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.button_audio:
                showSelect(FILE_SELECT_AUDIO);
                break;
            case R.id.button_video:
                showSelect(FILE_SELECT_VIDEO);
                break;
            case R.id.button_start:
                if (mAudioPath == null || mAudioPath.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "获取不到音频文件路径", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mVideoPath == null || mVideoPath.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "获取不到视频文件路径", Toast.LENGTH_SHORT).show();
                    return;
                }
                mButtonStart.setEnabled(false);
                new Thread(() -> {
                    try {
                        File file = new File(mComFile);
                        if (file.exists()) {
                            file.delete();
                        }
                        file.createNewFile();
                        int ret = MediaUtil.combineTwoVideos(mAudioPath, 0L, mVideoPath, file);
                        if (ret == 0) {
                            mButtonStart.post(new Runnable() {
                                @Override
                                public void run() {
                                    mButtonStart.setEnabled(true);
                                    Toast.makeText(getApplicationContext(), "完成", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }).start();

                break;
        }
    }

    private void showFileChooser(String title, int type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, title), type);
        } catch (ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            Log.d(TAG, "File Uri: " + uri.toString());
            switch (requestCode) {
                case FILE_SELECT_AUDIO:
                    mAudioPath = getPath(this, uri);
                    Log.d(TAG, "mAudioPath Path: " + mAudioPath);
                    break;
                case FILE_SELECT_VIDEO:
                    mVideoPath = getPath(this, uri);
                    Log.d(TAG, "mVideoPath Path: " + mVideoPath);
                    break;
                default:
                    break;
            }
        }

    }

    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it  Or Log it.
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

}
