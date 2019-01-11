package com.media.lingxiao.harddecoder.utils;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;


public class H264EncoderConsumer {
    private int m_width, m_height, m_framerate;
    private MediaCodec mediaCodec;
    private boolean isRuning;
    private static int yuvqueuesize = 10;
    private ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<>(yuvqueuesize);
    private byte[] configbyte;
    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/testYuv.h264";
    private static final int TIMEOUT_USEC = 12000;
    private BufferedOutputStream outputStream;
    private final MediaUtil mediaUtil;
    private static final String TAG = H264Encoder.class.getSimpleName();
    private MediaCodec.BufferInfo mbBufferInfo;

    private void createfile(){
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private int bit_rate = 3; //可以设置为 1 3 5
    public H264EncoderConsumer(int width, int height, int framerate, int bitrate,EncoderParams params) {
        m_width = width;
        m_height = height;
        m_framerate = framerate;

        mediaUtil = MediaUtil.getDefault();
        mediaUtil.setVideoPath(params.getVideoPath());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        //颜色空间设置为yuv420sp
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        //比特率，也就是码率 ，值越高视频画面更清晰画质更高
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * bit_rate);
        //帧率，一般设置为30帧就够了
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        //关键帧间隔  6.0以上无法控制，需要使用opengles渲染控制
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            //初始化mediacodec
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //设置为编码模式和编码格式
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        mbBufferInfo = new MediaCodec.BufferInfo();
        createfile();
    }

    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    public void putYUVData(byte[] buffer) {
        if (YUVQueue.size() >= yuvqueuesize) {
            YUVQueue.poll();
            Log.e(TAG, "丢帧");
        }
        YUVQueue.add(buffer);
    }

    private boolean isAddKeyFrame;
    public void StartEncoderThread(){
        Thread EncoderThread = new Thread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                isRuning = true;
                byte[] input = null;
                long pts =  0;
                long generateIndex = 0;

                while (isRuning) {
                    if (YUVQueue.size() > 0){
                        //从缓冲队列中取出一帧
                        input = YUVQueue.poll();
                        byte[] yuv420sp = new byte[m_width*m_height*3/2];
                        //把待编码的视频帧转换为YUV420格式
                        NV21ToNV12(input,yuv420sp,m_width,m_height);
                        input = yuv420sp;
                    }
                    if (input != null) {
                        try {
                            long startMs = System.currentTimeMillis();
                            //编码器输入缓冲区
                            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                            //编码器输出缓冲区
                            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                pts = computePresentationTime(generateIndex);
                                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                inputBuffer.clear();
                                //把转换后的YUV420格式的视频帧放到编码器输入缓冲区中
                                inputBuffer.put(input);
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                                generateIndex += 1;
                            }


                            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = -1;
                            do {
                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                    //Log.i(TAG, "获得编码器输出缓存区超时");
                                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                    // 如果API小于21，APP需要重新绑定编码器的输入缓存区；
                                    // 如果API大于21，则无需处理INFO_OUTPUT_BUFFERS_CHANGED
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                        outputBuffers = mediaCodec.getOutputBuffers();
                                    }
                                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    // 编码器输出缓存区格式改变，通常在存储数据之前且只会改变一次
                                    // 这里设置混合器视频轨道，如果音频已经添加则启动混合器（保证音视频同步）
                                    synchronized (H264EncoderConsumer.this) {
                                        MediaFormat newFormat = mediaCodec.getOutputFormat();
                                        mediaUtil.addTrack(newFormat, true);
                                    }

                                        //Log.i(TAG, "编码器输出缓存区格式改变，添加视频轨道到混合器");
                                } else {
                                    // 获取一个只读的输出缓存区inputBuffer ，它包含被编码好的数据
                                    //因为上面的addTrackIndex方法不一定会被调用,所以要在此处再判断并添加一次,这也是混合的难点之一
                                    if (!mediaUtil.isAddVideoTrack()){
                                        synchronized (H264EncoderConsumer.this) {
                                            MediaFormat newFormat = mediaCodec.getOutputFormat();
                                            mediaUtil.addTrack(newFormat, true);
                                        }
                                    }
                                    ByteBuffer outputBuffer = null;
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                        outputBuffer = outputBuffers[outputBufferIndex];
                                    } else {
                                        outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                                    }
                                    // 如果API<=19，需要根据BufferInfo的offset偏移量调整ByteBuffer的位置
                                    // 并且限定将要读取缓存区数据的长度，否则输出数据会混乱
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                                        outputBuffer.position(mBufferInfo.offset);
                                        outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                                    }
                                    mediaUtil.putStrem(outputBuffer, mBufferInfo, true);
                                    Log.i(TAG,"------编码混合视频数据-----" + mBufferInfo.size);
                                    // 处理结束，释放输出缓存区资源
                                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                }

                            } while (outputBufferIndex >= 0);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    } else {
                        try {
                            //这里可以根据实际情况调整编码速度
                            Thread.sleep(500);
                            Log.i(TAG, "调整编码速度");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        EncoderThread.start();
    }

    private void stopEncoder() {
        if (null != mediaCodec){
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    public void stopThread(){
        isRuning = false;
        try {
            stopEncoder();
            outputStream.flush();
            outputStream.close();
            h264Listener = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaUtil.release();
    }
    /**
     * 计算pts
     * @param frameIndex
     * @return
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / m_framerate;
    }

    public boolean isEncodering(){
        return isRuning;
    }

    private H264Encoder.PreviewFrameListener h264Listener;
    public void setPreviewListner(H264Encoder.PreviewFrameListener listener){
        this.h264Listener = listener;
    }
    public interface PreviewFrameListener {
        void onPreview(byte[] data,int width ,int height);
    }
}
