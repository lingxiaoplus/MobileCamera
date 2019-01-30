package com.media.lingxiao.harddecoder.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.media.lingxiao.harddecoder.R;
import com.media.lingxiao.harddecoder.decoder.AudioDecoder;
import com.media.lingxiao.harddecoder.decoder.H264Decoder;

import java.io.File;

public class SimpleVideoView extends FrameLayout implements SurfaceHolder.Callback,H264Decoder.VideoCallBack {
    private String videoPath = "";
    private static final String TAG = SimpleVideoView.class.getSimpleName();
    private SurfaceView mSurfaceView;
    private Object mSurfaceLock = new Object();

    public SimpleVideoView(Context context) {
        this(context,null);
    }

    public SimpleVideoView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SimpleVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View.inflate(context, R.layout.video_view,this);
        mSurfaceView = findViewById(R.id.surface_view);
        SurfaceHolder holder = mSurfaceView.getHolder();
        if (holder == null){
            Log.e(TAG, "初始化失败，holder is null");
            return;
        }
        holder.addCallback(this);
    }


    public void setDataSource(String videoPath){
        this.videoPath = videoPath;
        if (videoPath.isEmpty() || !new File(videoPath).exists()){
            Log.e(TAG, "播放失败，video path is null");
            return;
        }
        H264Decoder.getInstance().startDecodeFromMPEG_4(videoPath,mSurfaceView.getHolder().getSurface());
        AudioDecoder.getInstance().startDecodeFromMPEG_4(videoPath);
        H264Decoder.getInstance().setVideoCallBack(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCallback != null){
            mCallback.surfaceCreated();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        H264Decoder.getInstance().stopDecode();
        AudioDecoder.getInstance().stopDecode();
    }

    @Override
    public void onGetVideoInfo(final int width, final int height, float time) {
        SimpleVideoView.this.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
                float scale = (float) width / height;
                layoutParams.width = Math.min(width,getWidth());
                float scaleHeight = getWidth() * 1.0f / scale;
                layoutParams.height = (int) scaleHeight;
                mSurfaceView.setLayoutParams(layoutParams);
            }
        });
    }
    private VideoCallback mCallback;
    public interface VideoCallback{
        void surfaceCreated();
    }
    public void addCallback(VideoCallback callback){
        this.mCallback = callback;
    }
}
