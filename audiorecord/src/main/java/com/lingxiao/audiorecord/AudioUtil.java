package com.lingxiao.audiorecord;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioUtil {
    private static final String TAG = "AudioUtil";
    private AudioRecord audioRecord = null;  // 声明 AudioRecord 对象
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段
    //所有android系统都支持
    private int sampleRate = 44100;
    //单声道输入
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    //PCM_16是所有android系统都支持的
    private int autioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private long mStartTimeStamp;
    private File mAudioFile;
    private String mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioSimple/";
    private FileOutputStream mAudioFileOutput;
    private boolean isRecording = false;

    private static AudioUtil audioUtil;
    private boolean mIsPlaying;

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
    public boolean startAudioRecord(String fileName) {
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
                if (read <= 0){
                    return false;
                }else {
                    mAudioFileOutput.write(data,0,read);
                }
            }
            stopRecorder();
        } catch (IOException e) {
            e.printStackTrace();
            stopRecorder();
            return false;
        }
        return true;
    }

    public boolean stopAudioRecord(){
        isRecording = false;
        stopRecorder();
        try {
            mAudioFileOutput.flush();
            mAudioFileOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            int minBufferSize=AudioTrack.getMinBufferSize(sampleRate,channelConfig,audioFormat);
            byte data[] = new byte[minBufferSize];
            //构造AudioTrack  不能小于AudioTrack的最低要求，也不能小于我们每次读的大小
            AudioTrack audioTrack=new AudioTrack(streamType,sampleRate,channelConfig,audioFormat,
                    Math.max(minBufferSize,data.length),mode);

            //从文件流读数据
            FileInputStream inputStream=null;
            try{
                //循环读数据，写到播放器去播放
                inputStream=new FileInputStream(audioFile);

                //循环读数据，写到播放器去播放
                int read;
                //只要没读完，循环播放
                while ((read=inputStream.read(data))>0){
                    int ret=audioTrack.write(data,0,read);
                    //检查write的返回值，处理错误
                    switch (ret){
                        case AudioTrack.ERROR_INVALID_OPERATION:
                        case AudioTrack.ERROR_BAD_VALUE:
                        case AudioManager.ERROR_DEAD_OBJECT:
                            Log.d(TAG, "doPlay: 失败");
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
                mIsPlaying = false;
                //关闭文件输入流
                if(inputStream !=null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //播放器释放
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
        }
    }

    public boolean isRecording(){
        return isRecording;
    }
    public boolean isPlaying(){
        return mIsPlaying;
    }
    private void stopRecorder() {
        if (audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
}
