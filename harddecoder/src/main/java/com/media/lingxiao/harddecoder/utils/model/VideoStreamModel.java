package com.media.lingxiao.harddecoder.utils.model;

public class VideoStreamModel {
    private int type; //视频格式
    private int width; //视频宽
    private int height; //视频高
    private long seq_no0; //帧数 下同
    private long seq_no1;
    private byte[] video; //视频数据

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getSeq_no0() {
        return seq_no0;
    }

    public void setSeq_no0(long seq_no0) {
        this.seq_no0 = seq_no0;
    }

    public long getSeq_no1() {
        return seq_no1;
    }

    public void setSeq_no1(long seq_no1) {
        this.seq_no1 = seq_no1;
    }

    public byte[] getVideo() {
        return video;
    }

    public void setVideo(byte[] video) {
        this.video = video;
    }
}
