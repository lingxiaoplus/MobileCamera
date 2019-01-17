package com.media.lingxiao.harddecoder;

public class EncoderParams {
    // 码率等级
    public enum Quality{
        LOW, MIDDLE, HIGH
    }
    // 帧率
    public enum FrameRate{
        _20fps,_25fps,_30fps
    }
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100; //所有android系统都支持的采样率
    public static final int DEFAULT_CHANNEL_COUNT = 1; //单声道
    public static final int CHANNEL_COUNT_STEREO = 2;  //立体声
    public static final int DEFAULT_BIT_RATE = 96000;  //默认比特率
    private String videoPath;
    private int frameWidth;
    private int frameHeight;
    private int audioBitrate = DEFAULT_BIT_RATE;   // 音频编码比特率
    private int audioChannelCount = DEFAULT_CHANNEL_COUNT; // 通道数
    private int audioSampleRate = DEFAULT_AUDIO_SAMPLE_RATE;   // 采样率

    private int audioChannelConfig ; // 单声道或立体声
    private int audioFormat;    // 采样精度
    private int audioSouce;     // 音频来源

    private Quality bitRateQuality;   // 视频编码码率,0(低),1(中),2(高)
    private FrameRate frameRateDegree; // 视频编码帧率,0(低),1(中),2(高)

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

    public Quality getBitRateQuality() {
        return bitRateQuality;
    }

    public void setBitRateQuality(Quality bitRateQuality) {
        this.bitRateQuality = bitRateQuality;
    }

    public FrameRate getFrameRateDegree() {
        return frameRateDegree;
    }

    public void setFrameRateDegree(FrameRate frameRateDegree) {
        this.frameRateDegree = frameRateDegree;
    }
}
