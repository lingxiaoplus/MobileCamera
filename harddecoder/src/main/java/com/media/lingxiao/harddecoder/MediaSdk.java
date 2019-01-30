package com.media.lingxiao.harddecoder;

import android.util.Log;

import com.media.lingxiao.harddecoder.encoder.AudioEncoder;
import com.media.lingxiao.harddecoder.encoder.H264EncoderConsumer;

public class MediaSdk {
    public static final String TAG = MediaSdk.class.getSimpleName();
    public static void encodePcmToAac(EncoderParams params){
        if (AudioEncoder.isEncoding()){
            Log.i(TAG, "encode pcm to aac is already start");
            return;
        }
        AudioEncoder.getInstance()
                .setEncoderParams(params)
                .startEncodeAacData();
    }
    /*public static void dncodeAacToPcm(EncoderParams params){
            if (AudioEncoder.isEncoding()){
                Log.i(TAG, "encode pcm to aac is already start");
                return;
            }
            AudioEncoder.getInstance()
                    .setEncoderParams(params)
                    .startEncodeAacData(true);
        }*/


    public static void encodeYuvToH264(EncoderParams params){
        if (H264EncoderConsumer.isEncodering()){
            Log.i(TAG, "encode Yuv to H264 is already start");
            return;
        }
        H264EncoderConsumer.getInstance()
                .setEncoderParams(params)
                .StartEncodeH264Data();
    }


    public static void hardRecodeMp4(EncoderParams params){
        if (H264EncoderConsumer.isEncodering() || AudioEncoder.isEncoding()){
            Log.i(TAG, "Recode  is already start");
            return;
        }
        H264EncoderConsumer.getInstance()
                .setEncoderParams(params)
                .StartEncodeH264Data();
        AudioEncoder.getInstance()
                .setEncoderParams(params)
                .startEncodeAacData();
    }

}
