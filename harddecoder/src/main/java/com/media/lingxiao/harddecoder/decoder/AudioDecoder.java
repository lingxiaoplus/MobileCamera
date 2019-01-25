package com.media.lingxiao.harddecoder.decoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecoder {
    private static final int TIMEOUT_USEC = 12000;
    private MediaCodec mCodec;
    private static AudioDecoder mAudioDecoder;
    private final String mime = "audio/mp4a-latm";
    private MediaCodec.BufferInfo mBufferInfo;
    private BufferedOutputStream outputStream;
    private static boolean isDncoding = false;
    private AudioDecoder(){

    }
    public static AudioDecoder getInstance(){
        if (mAudioDecoder == null){
            synchronized (AudioDecoder.class){
                if (mAudioDecoder == null){
                    mAudioDecoder = new AudioDecoder();
                }
            }
        }
        return mAudioDecoder;
    }

    public AudioDecoder setPcmFilePth(String path){
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            e.printStackTrace();
        }
        return mAudioDecoder;
    }

    public void configDecode(){
        try {
            mCodec = MediaCodec.createEncoderByType(mime);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, mime);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1); //声道
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 100); //作用于inputBuffer的大小
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100); //采样率
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS,1); //用来标记AAC是否有adts头，1->有
            //ByteBuffer key
            byte[] data = new byte[]{(byte) 0x14, (byte) 0x08};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            //ADT头的解码信息
            mediaFormat.setByteBuffer("csd-0", csd_0);
            mCodec.configure(mediaFormat, null, null, 0);
            //编解码器缓冲区
            mBufferInfo = new MediaCodec.BufferInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startDecode(byte[] audioBuf,int length) throws IOException{
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();

        int inputBufIndex = mCodec.dequeueInputBuffer(-1);
        if (inputBufIndex > 0){
            ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
            inputBuffer.clear();
            inputBuffer.put(audioBuf);//添加数据
            inputBuffer.limit(audioBuf.length);//限制ByteBuffer的访问长度
            mCodec.queueInputBuffer(inputBufIndex,0,length,getPTSUs(),0);
        }
        int outputBufferIndex = mCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        ByteBuffer outputBuffer;
        while (outputBufferIndex >= 0) {
            //获取解码后的ByteBuffer
            outputBuffer = outputBuffers[outputBufferIndex];
            //用来保存解码后的数据
            byte[] outData = new byte[mBufferInfo.size];
            outputBuffer.get(outData);
            //清空缓存
            outputBuffer.clear();
            //播放解码后的数据
            outputStream.write(outData, 0, outData.length);
            //释放已经解码的buffer
            mCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        }
    }

    public void stopDecode(){
        isDncoding = false;
        if (null != mCodec){
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long prevPresentationTimes = 0;
    private long getPTSUs() {
        long result = System.nanoTime() / 1000;
        if (result < prevPresentationTimes) {
            result = (prevPresentationTimes - result) + result;
        }
        return result;
    }
}
