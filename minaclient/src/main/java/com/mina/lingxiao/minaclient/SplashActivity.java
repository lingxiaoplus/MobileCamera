package com.mina.lingxiao.minaclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 请输入ip+端口
     */
    private EditText mEditTextIp ,mEditPort;
    /**
     * 确定
     */
    private Button mButtonConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initView();
    }

    private void initView() {
        mEditTextIp = findViewById(R.id.editText_ip);
        mButtonConfirm = findViewById(R.id.button_confirm);
        mEditPort = findViewById(R.id.editText_port);
        mButtonConfirm.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.button_confirm:
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                intent.putExtra("ip",mEditTextIp.getText().toString().trim());
                intent.putExtra("port",mEditPort.getText().toString().trim());
                startActivity(intent);
                break;
        }
    }
}
