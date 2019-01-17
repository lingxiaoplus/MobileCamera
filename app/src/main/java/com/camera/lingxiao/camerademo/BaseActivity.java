package com.camera.lingxiao.camerademo;

import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.camera.lingxiao.camerademo.crash.AppManager;

public class BaseActivity extends AppCompatActivity {

    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getAppManager().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getAppManager().finishActivity(this);
    }


    public void showProgressDialog(){
        if (mDialog == null){
            mDialog = new ProgressDialog(this);
        }
        mDialog.setMessage("请稍后");
        mDialog.show();
    }

    public void cancelProgressDialog(){
        if (mDialog != null){
            mDialog.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
