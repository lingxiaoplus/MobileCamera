package com.media.lingxiao.harddecoder;

public class EncoderParams {
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100; //所有android系统都支持的采样率
    public static final int DEFAULT_CHANNEL_COUNT = 1; //单声道
    public static final int CHANNEL_COUNT_STEREO = 2;  //立体声
    public static final int DEFAULT_AUDIO_BIT_RATE = 96000;  //默认比特率

    public static final int LOW_VIDEO_BIT_RATE = 1;  //默认比特率
    public static final int MIDDLE_VIDEO_BIT_RATE = 3;  //默认比特率
    public static final int HIGH_VIDEO_BIT_RATE = 5;  //默认比特率

    private String videoPath;  //视频文件的全路径
    private String audioPath;  //音频文件全路径
    private int frameWidth;
    private int frameHeight;
    private int frameRate; // 帧率
    private int videoQuality = MIDDLE_VIDEO_BIT_RATE; //码率等级
    private int audioBitrate = DEFAULT_AUDIO_BIT_RATE;   // 音频编码比特率
    private int audioChannelCount = DEFAULT_CHANNEL_COUNT; // 通道数
    private int audioSampleRate = DEFAULT_AUDIO_SAMPLE_RATE;   // 采样率

    private int audioChannelConfig ; // 单声道或立体声
    private int audioFormat;    // 采样精度
    private int audioSouce;     // 音频来源


    public EncoderParams(){

    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public int getAudioChannelCount() {
        return audioChannelCount;
    }

    public void setAudioChannelCount(int audioChannelCount) {
        this.audioChannelCount = audioChannelCount;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioChannelConfig() {
        return audioChannelConfig;
    }

    public void setAudioChannelConfig(int audioChannelConfig) {
        this.audioChannelConfig = audioChannelConfig;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(int audioFormat) {
        this.audioFormat = audioFormat;
    }

    public int getAudioSouce() {
        return audioSouce;
    }

    public void setAudioSouce(int audioSouce) {
        this.audioSouce = audioSouce;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(int videoQuality) {
        this.videoQuality = videoQuality;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }
}
