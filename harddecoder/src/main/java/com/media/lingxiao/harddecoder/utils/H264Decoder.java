package com.media.lingxiao.harddecoder.utils;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class H264Decoder {
    private static final int TIMEOUT_USEC = 12000;
    private MediaCodec mCodec;
    private boolean isStart = false;
    public static final String TAG = H264Decoder.class.getSimpleName();
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
}
