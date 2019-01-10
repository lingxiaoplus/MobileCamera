package com.camera.lingxiao.camerademo.crash;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import com.camera.lingxiao.camerademo.R;
import com.camera.lingxiao.camerademo.utils.PrefUtils;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);
        TextView tvError = findViewById(R.id.tv_error);
        String errorInfo = PrefUtils.getString(this, ContentValue.ERRORSTR,"");
        tvError.setText(errorInfo);
    }
}
