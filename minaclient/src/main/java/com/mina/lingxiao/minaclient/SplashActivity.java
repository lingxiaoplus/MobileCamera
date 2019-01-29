package com.mina.lingxiao.minaclient;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.EasyPermissions;

public class SplashActivity extends AppCompatActivity implements View.OnClickListener , EasyPermissions.PermissionCallbacks{

    /**
     * 请输入ip+端口
     */
    private EditText mEditTextIp ,mEditPort;
    /**
     * 确定
     */
    private Button mButtonConfirm;

    public static final int RC_CAMERA_AND_LOCATION = 1;

    private String[] perms = {Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initView();

        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    RC_CAMERA_AND_LOCATION, perms);
        }
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

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
