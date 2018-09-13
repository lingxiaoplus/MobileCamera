package com.camera.lingxiao.camerademo.utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaUtil {
    /**
     * 将音频和视频进行合成
     * @param audioPath 提供音频的文件路径
     * @param audioStartTime 音频的开始时间
     * @param frameVideoPath 提供视频的文件路径
     * @param combinedVideoOutFile 保存的文件
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static int combineTwoVideos(String audioPath,
                                        long audioStartTime,
                                        String frameVideoPath,
                                        File combinedVideoOutFile) throws IOException {
        MediaMuxer mediaMuxer = new MediaMuxer(combinedVideoOutFile.getAbsolutePath(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4); //用于合成音频与视频

        MediaExtractor audioExtractor = new MediaExtractor(); //MediaExtractor作用是将音频和视频的数据进行分离
        audioExtractor.setDataSource(audioPath);
        int audioExtractorTrackIndex = -1; //提供音频的音频轨
        int audioMuxerTrackIndex = -1; //合成后的视频的音频轨
        int audioMaxInputSize = 0; //能获取的音频的最大值
        //多媒体流中video轨和audio轨的总个数
        for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
            MediaFormat format = audioExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);//主要描述mime类型的媒体格式
            if (mime.startsWith("audio/")) { //找到音轨
                //extractor.selectTrack(i);
                audioExtractorTrackIndex = i;
                audioMuxerTrackIndex = mediaMuxer.addTrack(format);//将音轨添加到MediaMuxer，并返回新的轨道
                audioMaxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);//得到能获取的有关音频的最大值
            }
        }

        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(frameVideoPath);
        int frameExtractorTrackIndex = -1; //视频轨
        int frameMuxerTrackIndex = -1; //合成后的视频的视频轨
        int frameMaxInputSize = 0; //能获取的视频的最大值
        int frameRate = 0; //视频的帧率
        long frameDuration = 0;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat videoFormate = videoExtractor.getTrackFormat(i);
            String videoMime = videoFormate.getString(MediaFormat.KEY_MIME);
            if (videoMime.startsWith("video/")){
                //extractor.selectTrack(i);
                frameExtractorTrackIndex = i;
                frameMuxerTrackIndex = mediaMuxer.addTrack(videoFormate);
                frameMaxInputSize = videoFormate.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                frameRate = videoFormate.getInteger(MediaFormat.KEY_FRAME_RATE);//获取视频的帧率
                frameDuration = videoFormate.getLong(MediaFormat.KEY_DURATION);//获取视频时长
            }
        }

        mediaMuxer.start(); //开始合成

        audioExtractor.selectTrack(audioExtractorTrackIndex); //选择想要处理的track
        MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer audioByteBuffer = ByteBuffer.allocate(audioMaxInputSize);
        while (true){
            //检索当前编码的样本并将其存储在字节缓冲区中
            int readSampleSize = audioExtractor.readSampleData(audioByteBuffer,0);
            if (readSampleSize < 0){
                //如果没有可获取的样本则退出循环
                audioExtractor.unselectTrack(audioExtractorTrackIndex);
                break;
            }

            long sampleTime = audioExtractor.getSampleTime();
            if (sampleTime < audioStartTime){
                //如果样本时间小于我们想要的开始时间就快进
                audioExtractor.advance();
                continue;
            }
            if (sampleTime > audioStartTime + frameDuration){
                //如果样本时间大于开始时间+视频时长，就退出循环
                break;
            }
            audioBufferInfo.size = readSampleSize;
            audioBufferInfo.offset = 0;
            audioBufferInfo.flags = audioExtractor.getSampleFlags();
            audioBufferInfo.presentationTimeUs = sampleTime - audioStartTime;
            mediaMuxer.writeSampleData(audioMuxerTrackIndex,audioByteBuffer,audioBufferInfo);
            audioExtractor.advance();
        }

        videoExtractor.selectTrack(frameExtractorTrackIndex); //选择想要处理的track
        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer videoByteBuffer = ByteBuffer.allocate(frameMaxInputSize);
        while (true){
            //检索当前编码的样本并将其存储在字节缓冲区中
            int readSampleSize = videoExtractor.readSampleData(videoByteBuffer,0);
            if (readSampleSize < 0){
                //如果没有可获取的样本则退出循环
                videoExtractor.unselectTrack(frameExtractorTrackIndex);
                break;
            }

            videoBufferInfo.size = readSampleSize;
            videoBufferInfo.offset = 0;
            videoBufferInfo.flags = videoExtractor.getSampleFlags();
            videoBufferInfo.presentationTimeUs = 1000*1000/frameRate;
            mediaMuxer.writeSampleData(frameMuxerTrackIndex,videoByteBuffer,videoBufferInfo);
            videoExtractor.advance();
        }

        audioExtractor.release();
        videoExtractor.release();
        mediaMuxer.release();
        return 0;
    }
}
