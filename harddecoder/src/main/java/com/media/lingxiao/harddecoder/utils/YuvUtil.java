package com.media.lingxiao.harddecoder.utils;

public class YuvUtil {
    static {
        System.loadLibrary("yuv-lib");
    }

    public static native byte[] rotateYuv90(byte[] nv21,int width,int height);
    public static native byte[] rotateYUVDegree270AndMirror(byte[] nv21,int width,int height);
    public static native void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height);
}
