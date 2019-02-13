package com.mina.lingxiao.minaclient;

import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.EasyPermissions;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import com.media.lingxiao.harddecoder.Client;

public class MainActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private Client mClient;
    private SurfaceHolder mHolder;
    private String mIp;
    private String mPort;
    private TextView mTextWidth,mTextHeight,mTextFps;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            VideoData videoData = (VideoData) msg.obj;
            mTextWidth.setText("视频宽："+videoData.getWidth());
            mTextHeight.setText("视频高："+videoData.getHeight());
            mTextFps.setText("视频帧："+videoData.getSeq_no());
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);
        mTextWidth = findViewById(R.id.tv_width);
        mTextHeight = findViewById(R.id.tv_height);
        mTextFps = findViewById(R.id.tv_fps);
        Intent intent = getIntent();
        mIp = intent.getStringExtra("ip");
        mPort = intent.getStringExtra("port");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new SurfaceCallBack());
    }
    private class SurfaceCallBack implements SurfaceHolder.Callback {
        VideoData videoData = new VideoData();
        @Override
        public void surfaceCreated(final SurfaceHolder surfaceHolder) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mClient = new Client(mIp,Integer.valueOf(mPort));
                    mClient.play(surfaceHolder,1080,1920);
                    mClient.setCameraDataCallback(new Client.CameraDataCallback() {
                        @Override
                        public void onH264DataFrame(byte[] h264, int width, int height, long seq_no) {
                            videoData.setWidth(width);
                            videoData.setHeight(height);
                            videoData.setSeq_no(seq_no);
                            Message message = mHandler.obtainMessage();
                            message.what = 0;
                            message.obj = videoData;
                            mHandler.sendMessage(message);
                        }
                    });
                }
            }).start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int formate, final int width, final int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            if (null != mClient){
                mClient.stopPlay();
            }
        }
    }

    private class VideoData{
        private int width;
        private int height;
        private long seq_no;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public long getSeq_no() {
            return seq_no;
        }

        public void setSeq_no(long seq_no) {
            this.seq_no = seq_no;
        }
    }
}
