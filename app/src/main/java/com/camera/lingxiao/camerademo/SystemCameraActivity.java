package com.camera.lingxiao.camerademo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static android.provider.MediaStore.AUTHORITY;

public class SystemCameraActivity extends AppCompatActivity {
    private String mDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SystemCapture";
    private ImageView mImageView;
    private File it;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_camera);

        File file = new File(mDir);
        if (!file.exists()) {
            file.mkdirs();
        } else {
            if (file.isFile()) {
                file.delete();
                file.mkdirs();
            }
        }

        mImageView = findViewById(R.id.imageView);
        Button btnCamera = findViewById(R.id.button_camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                it = new File(mDir, System.currentTimeMillis() + ".jpg");
                try {
                    if (it.exists()) {
                        it.delete();
                        it.createNewFile();
                    } else {
                        it.createNewFile();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {  //如果是7.0以上，使用FileProvider，否则会报错
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri imgUri = FileProvider.getUriForFile(getApplicationContext(), "com.camera.lingxiao.camerademo.fileProvider", it);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri); //设置拍照后图片保存的位置
                } else {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(it)); //设置拍照后图片保存的位置
                }
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString()); //设置图片保存的格式
                startActivityForResult(intent, 200); //调起系统相机
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 200) {
                Toast.makeText(this, "拍照成功", Toast.LENGTH_SHORT).show();
                Uri imgUri = FileProvider.getUriForFile(getApplicationContext(),
                        "com.camera.lingxiao.camerademo.fileProvider", it);
                gotoCrop(imgUri);
            } else if (requestCode == 201) {
                Bitmap bitmap = BitmapFactory.decodeFile(it.getAbsolutePath());
                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap);
                }
            }


        }
    }

    private void gotoCrop(Uri sourceUri) {
        //File imageCropFile = new File(mDir,System.currentTimeMillis()+"_crop_.jpg"); //创建一个保存裁剪后照片的File

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1); //X方向上的比例
        intent.putExtra("aspectY", 1);//Y方向上的比例
        intent.putExtra("outputX", 500);//裁剪区的宽
        intent.putExtra("outputY", 500);//裁剪区的高
        intent.putExtra("scale ", true);//是否保留比例
        intent.putExtra("return-data", false); //是否在Intent中返回图片
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString()); //设置输出图片的格式

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.setDataAndType(sourceUri, "image/*");  //设置数据源,必须是由FileProvider创建的ContentUri

            Uri imgCropUri = Uri.fromFile(it);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgCropUri);//设置输出  不需要ContentUri,否则失败
            Log.d("tag", "输入 $sourceUri");
            Log.d("tag", "输出 ${Uri.fromFile(it)}");
        } else {
            intent.setDataAndType(Uri.fromFile(it), "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(it));
        }
        startActivityForResult(intent, 201);
    }
}
