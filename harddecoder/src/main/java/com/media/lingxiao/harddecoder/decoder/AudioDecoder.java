package com.media.lingxiao.harddecoder.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.media.lingxiao.harddecoder.ApiException;
import com.media.lingxiao.harddecoder.EncoderParams;
import com.media.lingxiao.harddecoder.IoResultFunction;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AudioDecoder {
    private static final int TIMEOUT_USEC = 12000;
    private MediaCodec mCodec;
    private static AudioDecoder mAudioDecoder;
    private final String mime = "audio/mp4a-latm";
    private MediaCodec.BufferInfo mBufferInfo;
    private BufferedOutputStream outputStream;
    private static boolean isDecoding = false;
    private DataInputStream mInputStream;

    private static final String TAG = AudioDecoder.class.getSimpleName();
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

    private void createPcmFilePth(String path){
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

    public AudioDecoder setEncoderParams(EncoderParams params){
        try {
            mCodec = MediaCodec.createEncoderByType(mime);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, mime);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1); //声道
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 100); //作用于inputBuffer的大小
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, params.getAudioSampleRate()); //采样率
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS,1); //用来标记AAC是否有adts头，1->有
            //ByteBuffer key
            byte[] data = new byte[]{(byte) 0x14, (byte) 0x08};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            //ADT头的解码信息
            mediaFormat.setByteBuffer("csd-0", csd_0);
            mCodec.configure(mediaFormat, null, null, 0);
            //编解码器缓冲区
            mBufferInfo = new MediaCodec.BufferInfo();
            createPcmFilePth(params.getAudioPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mAudioDecoder;
    }


    private void decodeBytes(byte[] audioBuf,int length) throws IOException{
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

    public void startDecode(final String aacPath){
        if (!new File(aacPath).exists()){
            try {
                throw new FileNotFoundException("aac file not find");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        isDecoding = true;
        Thread audioDecodeTrhread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mInputStream = new DataInputStream(new FileInputStream(new File(aacPath)));
                    int size = 1024;
                    byte[] buf = new byte[size];
                    while (mInputStream.read(buf, 0, size) != -1 && isDecoding){
                        decodeBytes(buf,size);
                    }
                    stopDecodeSync();
                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        });
        audioDecodeTrhread.start();
    }
    private void stopDecodeSync(){
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
    public void stopDecode(){
        isDecoding = false;
    }


    public void startDecodeFromMPEG_4(final String MPEG_4_Path){
        if (!new File(MPEG_4_Path).exists()){
            try {
                throw new FileNotFoundException("MPEG_4 file not find");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        isDecoding = true;
        Thread audioDecodeTrhread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaExtractor audioExtractor = new MediaExtractor(); //MediaExtractor作用是将音频和视频的数据进行分离
                    audioExtractor.setDataSource(MPEG_4_Path);
                    AudioTrack audioTrack = null;
                    int audioExtractorTrackIndex = -1; //提供音频的音频轨
                    int audioMaxInputSize = 0; //能获取的音频的最大值
                    //多媒体流中video轨和audio轨的总个数
                    for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                        MediaFormat format = audioExtractor.getTrackFormat(i);
                        String mime = format.getString(MediaFormat.KEY_MIME);//主要描述mime类型的媒体格式
                        if (mime.startsWith("audio/")) { //找到音轨
                            audioExtractor.selectTrack(i);
                            int audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                            int audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                            int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate,
                                    (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                                    AudioFormat.ENCODING_PCM_16BIT);
                            int maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                            audioMaxInputSize = minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
                            int frameSizeInBytes = audioChannels * 2;
                            audioMaxInputSize = (audioMaxInputSize / frameSizeInBytes) * frameSizeInBytes;
                            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                    audioSampleRate,
                                    (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    audioMaxInputSize,
                                    AudioTrack.MODE_STREAM);
                            audioTrack.play();
                            try {
                                mCodec = MediaCodec.createDecoderByType(mime);
                                mCodec.configure(format, null, null, 0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (mCodec == null) {
                        Log.d(TAG, "audio decoder is unexpectedly null");
                        return;
                    }
                    mCodec.start();
                    /*final ByteBuffer[] buffers = mCodec.getOutputBuffers();
                    int sz = buffers[0].capacity();
                    if (sz <= 0) {
                        sz = audioMaxInputSize;
                    }*/

                    MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                    ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
                    while (isDecoding){
                        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            //检索当前编码的样本并将其存储在字节缓冲区中
                            int sampleSize = audioExtractor.readSampleData(inputBuffer, 0);
                            if (sampleSize < 0) {
                                //如果没有可获取的样本则退出循环
                                mCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                audioExtractor.unselectTrack(audioExtractorTrackIndex);
                                break;
                            } else {
                                mCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, audioExtractor.getSampleTime(), 0);
                                audioExtractor.advance();
                            }
                        }
                        int outputBufferIndex = mCodec.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_USEC);
                        while (outputBufferIndex >= 0) {
                            //获取解码后的ByteBuffer
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                            //用来保存解码后的数据
                            byte[] outData = new byte[audioBufferInfo.size];
                            outputBuffer.get(outData);
                            //清空缓存
                            outputBuffer.clear();
                            //播放解码后的数据
                            if (audioTrack != null){
                                audioTrack.write(outData,0,outData.length);
                            }
                            //释放已经解码的buffer
                            mCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mCodec.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_USEC);
                        }

                    }
                    if (audioTrack != null){
                        audioTrack.stop();
                        audioTrack.release();
                        audioTrack = null;
                    }
                    stopDecodeSync();
                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        });
        audioDecodeTrhread.start();
    }

    public static boolean isDncoding() {
        return isDecoding;
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
