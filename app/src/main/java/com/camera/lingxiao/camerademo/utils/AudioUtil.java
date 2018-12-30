package com.camera.lingxiao.camerademo.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.media.lingxiao.harddecoder.utils.AudioEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioUtil {
    private static final String TAG = "AudioUtil";
    private AudioRecord audioRecord = null;  // 声明 AudioRecord 对象
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段

    //所有android系统都支持  采样率：采样率越高，听到的声音和看到的图像就越连贯
    // 基本上高于44.1kHZ采样的声音，绝大部分人已经觉察不到其中的分别了
    private int sampleRate = 44100;
    //单声道输入
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    //PCM_16是所有android系统都支持的  16位的声音就是人类能听到的极限了，再高就听不见了 位数越高声音越清晰
    private int autioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private long mStartTimeStamp;
    private File mAudioFile;
    private String mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioSimple/";
    private FileOutputStream mAudioFileOutput; //存储录音文件
    private FileInputStream mAudioPlayInputStream; //播放录音文件
    private boolean isRecording = false;

    private static AudioUtil audioUtil;
    private boolean mIsPlaying;
    private AudioTrack mAudioTrack;
    private String mRecordFileName; //录音保存的文件路径
    private String mPlayFileName; //播放录音的文件路径

    private Runnable mAudioRunnableTask = new Runnable() {
        @Override
        public void run() {
            boolean result = startAudioRecord(mRecordFileName);
            if (result){
                Log.e(TAG, "录音结束");
            }else {
                Log.e(TAG, "录音失败");
            }
        }
    };

    private Runnable mAudioPlayRunnableTask = new Runnable() {
        @Override
        public void run() {
            File file = new File(mPlayFileName);
            doPlay(file);
        }
    };

    private AudioUtil(){}
    public static AudioUtil getInstance(){
        if (audioUtil == null){
            synchronized (AudioUtil.class){
                if (audioUtil == null){
                    audioUtil = new AudioUtil();
                }
            }
        }
        return audioUtil;
    }

    private AudioEncoder mAudioEncoder;
    private boolean startAudioRecord(String fileName) {
        isRecording = true;
        mStartTimeStamp = System.currentTimeMillis();
        mAudioFile = new File(mPath+fileName+mStartTimeStamp+".pcm");
        if (!mAudioFile.getParentFile().exists()){
            mAudioFile.getParentFile().mkdirs();
        }
        try {
            mAudioFile.createNewFile();
            //创建文件输出流
            mAudioFileOutput = new FileOutputStream(mAudioFile);

            //计算audioRecord能接受的最小的buffer大小
            recordBufSize = AudioRecord.getMinBufferSize(sampleRate,
                    channelConfig,
                    autioFormat);
            Log.e(TAG, "最小的buffer大小: " + recordBufSize);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    autioFormat, recordBufSize);
            //初始化一个buffer 用于从AudioRecord中读取声音数据到文件流
            byte data[] = new byte[recordBufSize];
            //开始录音
            audioRecord.startRecording();

            while (isRecording){
                //只要还在录音就一直读取
                int read = audioRecord.read(data,0,recordBufSize);
                if (read > 0){
                    mAudioFileOutput.write(data,0,read);
                    mAudioEncoder.putPcmData(data);
                }
            }
            //stopRecorder();
        } catch (IOException e) {
            e.printStackTrace();
            stopAudioRecord();
            return false;
        }
        return true;
    }

    public void doPlay(File audioFile) {
        if(audioFile !=null){
            mIsPlaying = true;
            //配置播放器
            //音乐类型，扬声器播放
            int streamType= AudioManager.STREAM_MUSIC;
            //录音时采用的采样频率，所以播放时同样的采样频率
            int sampleRate=44100;
            //单声道，和录音时设置的一样
            int channelConfig=AudioFormat.CHANNEL_OUT_MONO;
            //录音时使用16bit，所以播放时同样采用该方式
            int audioFormat=AudioFormat.ENCODING_PCM_16BIT;
            //流模式
            int mode= AudioTrack.MODE_STREAM;

            //计算最小buffer大小
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,channelConfig,audioFormat);

            byte data[] = new byte[minBufferSize];
            //构造AudioTrack  不能小于AudioTrack的最低要求，也不能小于我们每次读的大小
            mAudioTrack = new AudioTrack(streamType,sampleRate,channelConfig,audioFormat,
                    Math.max(minBufferSize,data.length),mode);

            //从文件流读数据
            try{
                //循环读数据，写到播放器去播放
                mAudioPlayInputStream = new FileInputStream(audioFile);

                //循环读数据，写到播放器去播放
                int read;
                //只要没读完，循环播放
                mAudioTrack.play();
                while (mIsPlaying){
                    int ret = 0;
                    read = mAudioPlayInputStream.read(data);
                    if (read > 0){
                        ret = mAudioTrack.write(data,0,read);
                    }
                    //mAudioFileOutput.write(data,0,read);
                    //检查write的返回值，处理错误
                    switch (ret){
                        case AudioTrack.ERROR_INVALID_OPERATION:
                        case AudioTrack.ERROR_BAD_VALUE:
                        case AudioManager.ERROR_DEAD_OBJECT:
                            Log.d(TAG, "doPlay: 失败,错误码："+ret);
                            return;
                        default:
                            break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                //读取失败
                Log.d(TAG, "doPlay: 失败");
            }finally {
                stopPlay();
                Log.d(TAG, "结束播放");
            }
        }
    }

    public void startRecord(String fileName){
        this.mRecordFileName = fileName;
        new Thread(mAudioRunnableTask).start();
        mAudioEncoder = new AudioEncoder();
        mAudioEncoder.startEncodeAacData();
    }

    public void startPlay(String fileName){
        this.mPlayFileName = fileName;
        new Thread(mAudioPlayRunnableTask).start();
    }
    public boolean stopAudioRecord(){
        isRecording = false;
        if (audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (mAudioEncoder != null){
            mAudioEncoder.stopEncodeAac();
        }
        try {
            mAudioFileOutput.flush();
            mAudioFileOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public boolean isRecording(){
        return isRecording;
    }
    public boolean isPlaying(){
        return mIsPlaying;
    }
    public void stopPlay(){
        mIsPlaying = false;
        //播放器释放
        if(mAudioTrack != null){
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
        //关闭文件输入流
        if(mAudioPlayInputStream !=null){
            try {
                mAudioPlayInputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private AudioListener mAudioListener;
    public void setAudioListener(AudioListener listener){
        this.mAudioListener = listener;
    }
    public interface AudioListener{
        void onRecordFinish();
        void onPlayFinish();
    }
}
