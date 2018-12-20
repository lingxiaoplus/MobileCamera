package com.media.lingxiao.harddecoder.utils;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;

public class AudioEncoder {
    private String MIME_TYPE = "audio/mp4a-latm";
    private int KEY_CHANNEL_COUNT = 2;
    private int KEY_SAMPLE_RATE = 44100;
    private int KEY_BIT_RATE = 64000;
    private int KEY_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    private int WAIT_TIME = 10000;

    private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int CHANNEL_MODE = AudioFormat.CHANNEL_IN_STEREO;

    private int BUFFFER_SIZE = 2048;

    
}
