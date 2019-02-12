package com.media.lingxiao.harddecoder.encoder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.media.lingxiao.harddecoder.EncoderParams;
import com.media.lingxiao.harddecoder.utils.MediaUtil;
import com.media.lingxiao.harddecoder.utils.YuvUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;


public class H264EncoderConsumer {
    private MediaCodec mediaCodec;
    private static volatile boolean isRuning = false;
    private static int yuvqueuesize = 10;
    private ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<>(yuvqueuesize);
    private byte[] configbyte;
    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/testYuv.h264";
    private static final int TIMEOUT_USEC = 10000;
    private BufferedOutputStream outputStream;
    private MediaUtil mediaUtil;
    private static final String TAG = H264EncoderConsumer.class.getSimpleName();
    //private MediaCodec.BufferInfo mbBufferInfo;
    private static H264EncoderConsumer mH264Encoder;
    private EncoderParams mEncoderParams;

    public H264EncoderConsumer(){

    }
    public static H264EncoderConsumer getInstance(){
        if (mH264Encoder == null){
            synchronized (H264EncoderConsumer.class){
                if (mH264Encoder == null){
                    mH264Encoder = new H264EncoderConsumer();
                }
            }
        }
        return mH264Encoder;
    }

    public H264EncoderConsumer setEncoderParams(EncoderParams params){
        this.mEncoderParams = params;
        int width = params.getFrameWidth();
        int height = params.getFrameHeight();
        mediaUtil = MediaUtil.getDefault();
        mediaUtil.setVideoPath(params.getVideoPath());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        //颜色空间设置为yuv420sp
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        //比特率，也就是码率 ，值越高视频画面更清晰画质更高
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * params.getVideoQuality());
        //帧率，一般设置为30帧就够了
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, params.getFrameRate());
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
        //mbBufferInfo = new MediaCodec.BufferInfo();
        createfile();
        return mH264Encoder;
    }

    public void addYUVData(byte[] yuvData){
        if (!isRuning && mediaCodec == null){
            return;
        }
        //byte[] yuv420sp = new byte[mEncoderParams.getFrameWidth() * mEncoderParams.getFrameHeight() * 3/2];
        //long before = System.currentTimeMillis();
        //NV21ToNV12(yuvData,yuv420sp,m_width,m_height);  //耗时110ms左右
        //YuvUtil.NV21ToNV12(yuvData,yuv420sp,mEncoderParams.getFrameWidth(),mEncoderParams.getFrameHeight()); //耗时35ms左右 解决卡顿问题
        //long after = System.currentTimeMillis();
        //Log.e(TAG, "nv21转nv12耗时: "+(after-before)+"ms");
        feedMediaCodecData(yuvData);
    }

    private void feedMediaCodecData(byte[] data) {
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
            // 绑定一个被空的、可写的输入缓存区inputBuffer到客户端
            ByteBuffer inputBuffer = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = inputBuffers[inputBufferIndex];
            } else {
                inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            }
            // 向输入缓存区写入有效原始数据，并提交到编码器中进行编码处理
            inputBuffer.clear();
            inputBuffer.put(data);
            inputBuffer.clear();
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, System.nanoTime() / 1000, BUFFER_FLAG_KEY_FRAME);
        }
    }

    public void StartEncodeH264Data(){
        Thread EncoderThread = new Thread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                isRuning = true;
                long pts =  0;
                long generateIndex = 0;
                byte[] input = null;
                boolean isAddKeyFrame = false;
                while (isRuning) {
                        try {
                            //编码器输出缓冲区
                            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex;
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

                                    // 判断输出数据是否为关键帧 必须在关键帧添加之后，再添加普通帧，不然会出现马赛克
                                    boolean keyFrame = (mBufferInfo.flags & BUFFER_FLAG_KEY_FRAME) != 0;
                                    if (keyFrame){
                                        // 录像时，第1秒画面会静止，这是由于音视轨没有完全被添加
                                        Log.i(TAG,"编码混合,视频关键帧数据(I帧)");
                                        mediaUtil.putStrem(outputBuffer, mBufferInfo, true);
                                        isAddKeyFrame = true;
                                    }else {
                                        // 添加视频流到混合器
                                        if(isAddKeyFrame){
                                            Log.i(TAG,"编码混合,视频普通帧数据(B帧,P帧)" + mBufferInfo.size);
                                            mediaUtil.putStrem(outputBuffer, mBufferInfo, true);

                                        }
                                    }

                                    // TODO: 2019/2/12 只有用下面的方法回调保存数据，才能在surface中显示 
                                    byte[] outData = new byte[mBufferInfo.size];
                                    //从buff中读取数据到outData中
                                    outputBuffer.get(outData);
                                    if(mBufferInfo.flags == BUFFER_FLAG_CODEC_CONFIG){
                                        configbyte = new byte[mBufferInfo.size];
                                        configbyte = outData;
                                    }else if(mBufferInfo.flags == BUFFER_FLAG_KEY_FRAME){
                                        byte[] keyframe = new byte[mBufferInfo.size + configbyte.length];
                                        System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                                        //把编码后的视频帧从编码器输出缓冲区中拷贝出来
                                        System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);

                                        //outputStream.write(keyframe,0,keyframe.length);
                                        if (h264Listener != null){
                                            h264Listener.onGetH264(keyframe,
                                                    mEncoderParams.getFrameWidth(),mEncoderParams.getFrameHeight());
                                        }
                                    }else{
                                        //outputStream.write(outData,0,outData.length);
                                        if (h264Listener != null){
                                            h264Listener.onGetH264(outData,
                                                    mEncoderParams.getFrameWidth(),mEncoderParams.getFrameHeight());
                                        }
                                    }

                                    // 处理结束，释放输出缓存区资源
                                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                }

                            } while (outputBufferIndex >= 0);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                }
                stopEncodeH264Sync();
            }
        });
        EncoderThread.start();
    }
    public void stopEncodeH264(){
        isRuning = false;
    }
    private void stopEncodeH264Sync(){
        try {
            if (null != mediaCodec){
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
            outputStream.flush();
            outputStream.close();
            h264Listener = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaUtil.release();
        if (h264Listener != null){
            h264Listener.onStopEncodeH264Success();
        }
    }

    /**
     * 计算pts
     * @param frameIndex
     * @return
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mEncoderParams.getFrameRate();
    }

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

    public static boolean isEncodering(){
        return isRuning;
    }

    private H264FrameListener h264Listener;
    public void setEncodeH264Listner(H264FrameListener listener){
        this.h264Listener = listener;
    }
    public interface H264FrameListener {
        void onGetH264(byte[] data,int width ,int height);
        void onStopEncodeH264Success();
    }

}
