package com.media.lingxiao.harddecoder.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class AudioEncoder {
    private MediaCodec.BufferInfo mBufferInfo;
    private final String mime = "audio/mp4a-latm";
    private int bitRate = 96000;
    private ByteBuffer[] inputBufferArray;
    private ByteBuffer[] outputBufferArray;
    private FileOutputStream  fileOutputStream;
    private MediaCodec mMediaCodec;

    private boolean isEncoding;
    private static final String TAG = AudioEncoder.class.getSimpleName();
    private MediaUtil mediaUtil;
    private AudioRecord mAudioRecord;
    private int mAudioRecordBufferSize;

    public AudioEncoder(EncoderParams params){
        try {
            mediaUtil = MediaUtil.getDefault();

            File root = Environment.getExternalStorageDirectory();
            File  fileAAc = new File(root,"生成的aac.aac");
            if(!fileAAc.exists()){
                fileAAc.createNewFile();
            }
            fileOutputStream = new FileOutputStream(fileAAc.getAbsoluteFile());
            mMediaCodec = MediaCodec.createEncoderByType(mime);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, mime);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2); //声道
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 100);//作用于inputBuffer的大小
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);//采样率

            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //start（）后进入执行状态，才能做后续的操作
            mMediaCodec.start();
            //获取输入缓存，输出缓存
            inputBufferArray = mMediaCodec.getInputBuffers();
            outputBufferArray = mMediaCodec.getOutputBuffers();
            startAudioRecord(params);

            mBufferInfo = new MediaCodec.BufferInfo();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startAudioRecord(EncoderParams params){
        // 计算AudioRecord所需输入缓存空间大小
        mAudioRecordBufferSize = AudioRecord.getMinBufferSize(params.getAudioSampleRate(),params.getAudioChannelConfig(),
                params.getAudioFormat());
        if(mAudioRecordBufferSize < 1600){
            mAudioRecordBufferSize = 1600;
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        mAudioRecord = new AudioRecord(params.getAudioSouce(),params.getAudioSampleRate(),
                params.getAudioChannelConfig(),params.getAudioFormat(), mAudioRecordBufferSize);
        // 开始录音
        mAudioRecord.startRecording();
    }

    public void stopAudioRecord(){
        if(mAudioRecord != null){
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    public void startEncodeAacData() {
        isEncoding = true;
        Thread aacEncoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isEncoding){
                    if (mAudioRecord != null && mMediaCodec != null){
                        byte[] audioBuf = new byte[mAudioRecordBufferSize];
                        int readBytes = mAudioRecord.read(audioBuf,0,mAudioRecordBufferSize);
                        if (readBytes > 0){
                            /*try {
                                fileOutputStream.write(audioBuf,0,readBytes);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }*/
                            try {
                                encodeAudioBytes(audioBuf,readBytes);
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                        }
                    }
                }
                stopAudioRecord();
                stopEncodeAac();
            }
        });
        aacEncoderThread.start();

    }

    private void encodeAudioBytes(byte[] audioBuf, int readBytes) {

        //dequeueInputBuffer（time）需要传入一个时间值，-1表示一直等待，0表示不等待有可能会丢帧，其他表示等待多少毫秒
        int inputIndex = mMediaCodec.dequeueInputBuffer(-1);//获取输入缓存的index
        if (inputIndex >= 0) {
            ByteBuffer inputByteBuf = inputBufferArray[inputIndex];
            if (audioBuf == null || readBytes <= 0){
                mMediaCodec.queueInputBuffer(inputIndex,0,0,getPTSUs(),MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else {
                inputByteBuf.clear();
                inputByteBuf.put(audioBuf);//添加数据
                inputByteBuf.limit(audioBuf.length);//限制ByteBuffer的访问长度
                mMediaCodec.queueInputBuffer(inputIndex, 0, readBytes, getPTSUs(), 0);//把输入缓存塞回去给MediaCodec
            }
        }

        int outputIndex = -1;
        do {
            outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER){
                //Log.i(TAG,"获得编码器输出缓存区超时");
            }else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                //设置混合器视频轨道，如果音频已经添加则启动混合器（保证音视频同步）
                synchronized (AudioEncoder.class){
                    MediaFormat format = mMediaCodec.getOutputFormat();
                    mediaUtil.addTrack(format,false);
                }
            }else {
                //获取缓存信息的长度
                int byteBufSize = mBufferInfo.size;
                // 当flag属性置为BUFFER_FLAG_CODEC_CONFIG后，说明输出缓存区的数据已经被消费了
            if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                Log.i(TAG,"编码数据被消费，BufferInfo的size属性置0");
                byteBufSize = 0;
            }
                // 数据流结束标志，结束本次循环
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.i(TAG,"数据流结束，退出循环");
                    break;
                }
                ByteBuffer outPutBuf = outputBufferArray[outputIndex];
                if (byteBufSize != 0){

                    //因为上面的addTrackIndex方法不一定会被调用,所以要在此处再判断并添加一次,这也是混合的难点之一
                    if (!mediaUtil.isAddAudioTrack()){
                        synchronized (AudioEncoder.this) {
                            MediaFormat newFormat = mMediaCodec.getOutputFormat();
                            mediaUtil.addTrack(newFormat, false);
                        }
                    }

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT){
                        outPutBuf.position(mBufferInfo.offset);
                        outPutBuf.limit(mBufferInfo.offset + mBufferInfo.size);
                    }
                    mediaUtil.putStrem(outPutBuf,mBufferInfo,false);
                    Log.i(TAG,"------编码混合音频数据-----" + mBufferInfo.size);
                }
                //释放
                mMediaCodec.releaseOutputBuffer(outputIndex, false);
            }
        } while (outputIndex >= 0 && isEncoding);
    }

    public void stopEncodeAac(){
        stopAudioRecord();
        if (mMediaCodec != null){
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
            isEncoding = false;
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mediaUtil.release();
    }

    private long prevPresentationTimes = 0;
    private long getPTSUs(){
        long result = System.nanoTime() / 1000;
        if(result < prevPresentationTimes){
            result = (prevPresentationTimes  - result ) + result;
        }
        return result;
    }

}
