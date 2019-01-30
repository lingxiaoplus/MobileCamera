package com.media.lingxiao.harddecoder.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class H264Decoder {
    private static final int TIMEOUT_USEC = 12000;
    private MediaCodec mCodec;
    private boolean isStart = false;
    public static final String TAG = H264Decoder.class.getSimpleName();
    private static boolean isDecoding = false;
    private static boolean isPause = false;
    private static H264Decoder mH264Decoder;
    private H264Decoder(){

    }
    public static H264Decoder getInstance(){
        if (mH264Decoder == null){
            synchronized (H264Decoder.class){
                if (mH264Decoder == null){
                    mH264Decoder = new H264Decoder();
                }
            }
        }
        return mH264Decoder;
    }

    public void play(SurfaceHolder holder,int width,int height) {
        try {
            Log.d(TAG, "播放的宽: "+width+"   高："+height);
            mCodec = MediaCodec.createDecoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            mCodec.configure(mediaFormat, holder.getSurface(), null, 0);
            mCodec.start();
            isStart = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (isStart) {
            mCodec.stop();
            //释放资源
            mCodec.release();
            isStart = false;
        }

    }

    public void stop(){
        if (mCodec != null){
            mCodec.stop();
            mCodec.release();
            mCodec = null;
            isStart = false;
        }
    }
    private byte[] lastBuf;
    private List<byte[]> nals = new ArrayList<>(30);
    private boolean csdSet = false; // sps和pps是否设置，在mediacodec里面可以不显式设置到mediaformat，但需要在buffer的前两帧给出

    public synchronized void handleH264(byte[] buffer) {
        byte[] typedAr = null;
        if (buffer == null || buffer.length < 1) return;
        if (lastBuf != null) {
            typedAr = new byte[buffer.length + lastBuf.length];
            System.arraycopy(lastBuf, 0, typedAr, 0, lastBuf.length);
            System.arraycopy(buffer, 0, typedAr, lastBuf.length, buffer.length);
        } else {
            typedAr = buffer;
        }
        int lastNalEndPos = 0;
        byte b1 = -1; // 前一个
        byte b2 = -1; // 前二个
        List<Integer> nalStartPos = new ArrayList<Integer>();
        for (int i = 0; i < typedAr.length - 1; i += 2) { // 可能出现的bug，length小于2
            byte b_0 = typedAr[i];
            byte b_1 = typedAr[i + 1];
            if (b_0 == 1 && b1 == 0 && b2 == 0) {
                nalStartPos.add(i - 3);
            } else if (b_1 == 1 && b_0 == 0 && b1 == 0 && b2 == 0) {
                nalStartPos.add(i - 2);
            }
            b2 = b_0;
            b1 = b_1;
        }
        if (nalStartPos.size() > 1) {
            for (int i = 0; i < nalStartPos.size() - 1; ++i) {
                byte[] nal = new byte[nalStartPos.get(i + 1) - nalStartPos.get(i)];
                System.arraycopy(typedAr, nalStartPos.get(i), nal, 0, nal.length);
                nals.add(nal);
                lastNalEndPos = nalStartPos.get(i + 1);
            }
        } else {
            lastNalEndPos = nalStartPos.get(0);
        }
        if (lastNalEndPos != 0 && lastNalEndPos < typedAr.length) {
            lastBuf = new byte[typedAr.length - lastNalEndPos];
            System.arraycopy(typedAr, lastNalEndPos, lastBuf, 0, typedAr.length - lastNalEndPos);
        } else {
            if (lastBuf == null) {
                lastBuf = typedAr;
            }
            byte[] _newBuf = new byte[lastBuf.length + buffer.length];
            System.arraycopy(lastBuf, 0, _newBuf, 0, lastBuf.length);
            System.arraycopy(buffer, 0, _newBuf, lastBuf.length, buffer.length);
            lastBuf = _newBuf;
        }
        boolean sps = false;
        boolean pps = false;
        boolean lastSps = false;
        if (!csdSet) {
            if (nals.size() > 0) {
                Iterator<byte[]> it = nals.iterator();
                while (it.hasNext() && !csdSet) {
                    byte[] nal = it.next();
                    if ((nal[4] & 0x1f) == 7) {
                        sps = true;
                        lastSps = true;
                        continue;
                    }
                    if ((nal[4] & 0x1f) == 8 && lastSps) {
                        pps = true;
                        csdSet = true;
                        continue;
                    }
                    it.remove();
                }
            }
        }
        if (!csdSet)
            nals.clear();
        if (nals.size() > 0) {
            Iterator<byte[]> it = nals.iterator();
            while (it.hasNext()) {
                ByteBuffer inputBuffer;
                //在给指定Index的inputbuffer[]填充数据后，调用这个函数把数据传给解码器
                int inputBufferIndex = mCodec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufferIndex >= 0) {
                    // 版本判断。当手机系统小于 5.0 时，用arras
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        inputBuffer = mCodec.getInputBuffer(inputBufferIndex);
                    } else {
                        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                        inputBuffer = inputBuffers[inputBufferIndex];
                    }
                    byte[] nal = it.next();
                    inputBuffer.put(nal);
                    mCodec.queueInputBuffer(inputBufferIndex, 0, nal.length, 0, 0);
                    it.remove();
                    continue;
                }
                break;
            }
        }
        /**
         *清理内存
         */
        if (nals.size() >30){
            int idx = 0;
            while (idx++<30){
                nals.remove(0);
            }
            lastBuf = null;
        }
        //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //获取解码得到的byte[]数据 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
        //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        while (outputBufferIndex >= 0) {//每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
            //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }



    private Surface mSurface;
    public void startDecodeFromMPEG_4(final String MPEG_4_Path, Surface surface){
        if (!new File(MPEG_4_Path).exists()){
            try {
                throw new FileNotFoundException("MPEG_4 file not find");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        this.mSurface = surface;
        isDecoding = true;
        Thread mediaDecodeTrhread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaExtractor videoExtractor = new MediaExtractor(); //MediaExtractor作用是将音频和视频的数据进行分离
                    videoExtractor.setDataSource(MPEG_4_Path);

                    int videoTrackIndex = -1; //提供音频的音频轨
                    //多媒体流中video轨和audio轨的总个数
                    for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                        MediaFormat format = videoExtractor.getTrackFormat(i);
                        String mime = format.getString(MediaFormat.KEY_MIME);//主要描述mime类型的媒体格式
                        if (mime.startsWith("video/")) { //找到音轨
                            videoExtractor.selectTrack(i);
                            videoTrackIndex = i;
                            int width = format.getInteger(MediaFormat.KEY_WIDTH);
                            int height = format.getInteger(MediaFormat.KEY_HEIGHT);
                            float time = format.getLong(MediaFormat.KEY_DURATION) / 1000000;
                            try {
                                mCodec = MediaCodec.createDecoderByType(mime);
                                mCodec.configure(format, mSurface, null, 0);
                                if (mVideoCallBack != null){
                                    mVideoCallBack.onGetVideoInfo(width,height,time);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                    if (mCodec == null) {
                        Log.d(TAG, "video decoder is unexpectedly null");
                        return;
                    }
                    mCodec.start();

                    MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
                    long startTimeStamp = System.currentTimeMillis(); //记录开始解码的时间
                    while (isDecoding){
                        // 暂停
                        if (isPause) {
                            continue;
                        }
                        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                inputBuffer = mCodec.getInputBuffer(inputBufferIndex);
                            }else {
                                ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                                inputBuffer = inputBuffers[inputBufferIndex];
                            }
                            //检索当前编码的样本并将其存储在字节缓冲区中
                            int sampleSize = videoExtractor.readSampleData(inputBuffer, 0);
                            if (sampleSize < 0) {
                                //如果没有可获取的样本则退出循环
                                mCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                videoExtractor.unselectTrack(videoTrackIndex);
                                break;
                            } else {
                                mCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, videoExtractor.getSampleTime(), 0);
                                videoExtractor.advance();
                            }
                        }
                        int outputBufferIndex = mCodec.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_USEC);
                        while (outputBufferIndex >= 0) {
                            decodeDelay(videoBufferInfo, startTimeStamp);
                            mCodec.releaseOutputBuffer(outputBufferIndex, true);
                            outputBufferIndex = mCodec.dequeueOutputBuffer(videoBufferInfo, 0);
                        }

                    }
                    stopDecodeSync();
                    videoExtractor.release();
                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        });
        mediaDecodeTrhread.start();
    }

    /**
     * 延迟解码
     * @param bufferInfo
     * @param startMillis
     */
    private void decodeDelay(MediaCodec.BufferInfo bufferInfo, long startMillis) {
        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMillis) {
            try {
                long current = bufferInfo.presentationTimeUs / 1000 - (System.currentTimeMillis() - startMillis);
                //Log.d(TAG, "decodeDelay: " + current + "ms");
                Thread.sleep(current);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void stopDecodeSync(){
        if (null != mCodec){
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }

    public static void pause() {
        isPause = true;
    }

    public void stopDecode(){
        isDecoding = false;
    }

    private VideoCallBack mVideoCallBack;
    public void setVideoCallBack(VideoCallBack callBack){
        this.mVideoCallBack = callBack;
    }
    public interface VideoCallBack{
        void onGetVideoInfo(int width,int height,float time);
    }

}
