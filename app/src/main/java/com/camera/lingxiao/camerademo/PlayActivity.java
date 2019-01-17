package com.camera.lingxiao.camerademo;

import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.media.lingxiao.harddecoder.decoder.H264Decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class PlayActivity extends BaseActivity {

    private SurfaceView mSurfaceView;
    private DataInputStream mInputStream;
    private SurfaceHolder mHolder;
    private H264Decoder mDecoder;
    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/testYuv.h264";

    private MediaCodec mCodec;
    private boolean mStopFlag = false;
    private Thread mDecodeThread;
    private Boolean isUsePpsAndSps = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        mSurfaceView = findViewById(R.id.surfaceView_play);
        Intent intent = getIntent();
        path = intent.getStringExtra("path");
        File f = new File(path);
        if (null == f || !f.exists() || f.length() == 0) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            //获取文件输入流
            mInputStream = new DataInputStream(new FileInputStream(new File(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            try {
                mInputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new SurfaceCallback());
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            createSurface();
            /*mDecoder = new H264Decoder();
            mDecoder.play(holder,1920,1080);
            try {
                mDecoder.handleH264(FileUtil.getBytes(mInputStream));
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }


    /**
     * @description 解码线程
     * @time 2016/12/19 16:36
     */
    private class DecodeThread implements Runnable {
        @Override
        public void run() {
            try {
                //解码细节
                decodeLoop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void decodeLoop() {
            //存放目标文件的数据
            ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
            //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long startMs = System.currentTimeMillis();
            long timeoutUs = 10000;
            byte[] marker0 = new byte[]{0, 0, 0, 1}; //相当于每帧之间的分隔符
            byte[] dummyFrame = new byte[]{0x00, 0x00, 0x01, 0x20};
            byte[] streamBuffer = null;
            try {
                streamBuffer = getBytes(mInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int bytes_cnt = 0;
            while (mStopFlag == false) {
                bytes_cnt = streamBuffer.length;
                if (bytes_cnt == 0) {
                    streamBuffer = dummyFrame;
                }

                int startIndex = 0;
                int remaining = bytes_cnt;
                while (true) {
                    if (remaining == 0 || startIndex >= remaining) {
                        break;
                    }
                    int nextFrameStart = KMPMatch(marker0, streamBuffer, startIndex + 2, remaining);
                    if (nextFrameStart == -1) {
                        nextFrameStart = remaining;
                    }

                    //1.准备填充器 该方法返回填充有效数据的输入缓冲区的索引 如果当前没有此类缓冲区，则返回-1。
                    int inIndex = mCodec.dequeueInputBuffer(timeoutUs);
                    if (inIndex >= 0) {
                        //2.填充数据
                        ByteBuffer byteBuffer = inputBuffers[inIndex];
                        byteBuffer.clear();
                        byteBuffer.put(streamBuffer, startIndex, nextFrameStart - startIndex);
                        //3.在给指定Index的inputbuffer[]填充数据后，调用这个函数把数据传给解码器
                        mCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startIndex, 0, 0);
                        startIndex = nextFrameStart;
                    } else {
                        continue;
                    }
                    //4 开始解码 第二个参数是最多阻塞timeoutUs微秒
                    int outIndex = mCodec.dequeueOutputBuffer(info, timeoutUs);
                    if (outIndex >= 0) {
                        //帧控制是不在这种情况下工作，因为没有PTS H264是可用的
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                //这里可以根据实际情况调整解码速度
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        boolean doRender = (info.size != 0);
                        //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
                        //调用这个api之后，SurfaceView才有图像
                        mCodec.releaseOutputBuffer(outIndex, doRender);
                    }

                }
                mStopFlag = true;
            }
        }
    }

    /**
     * 从文件中读取字节数组
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] getBytes(InputStream is) throws IOException {
        int len;
        int size = 1024;
        byte[] buf;
        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
            len = is.read(buf, 0, size);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1)
                bos.write(buf, 0, len);
            buf = bos.toByteArray();
        }
        return buf;
    }

    /**
     * kpm字符串匹配算法 通过分隔符匹配出下一帧的位置
     * @param pattern 0 0 0 1
     * @param bytes 文件流数据
     * @param start
     * @param remain 文件流大小
     * @return
     */
    private int KMPMatch(byte[] pattern, byte[] bytes, int start, int remain) {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int[] lsp = computeLspTable(pattern);

        int j = 0;  // Number of chars matched in pattern
        for (int i = start; i < remain; i++) {
            while (j > 0 && bytes[i] != pattern[j]) {
                // Fall back in the pattern
                j = lsp[j - 1];  // Strictly decreasing
            }
            if (bytes[i] == pattern[j]) {
                // Next char matched, increment position
                j++;
                if (j == pattern.length)
                    return i - (j - 1);
            }
        }

        return -1;  // Not found
    }

    private int[] computeLspTable(byte[] pattern) {
        int[] lsp = new int[pattern.length];
        lsp[0] = 0;  // Base case
        for (int i = 1; i < pattern.length; i++) {
            // Start by assuming we're extending the previous LSP
            int j = lsp[i - 1];
            while (j > 0 && pattern[i] != pattern[j])
                j = lsp[j - 1];
            if (pattern[i] == pattern[j])
                j++;
            lsp[i] = j;
        }
        return lsp;
    }


    private void createSurface() {
        try {
            //通过多媒体格式名创建一个可用的解码器
            mCodec = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //初始化编码器
        final MediaFormat mediaformat = MediaFormat.createVideoFormat("video/avc", 1080, 1920);
        //获取h264中的pps及sps数据
        if (isUsePpsAndSps) {
            byte[] header_sps = {0, 0, 0, 1, 103, 66, 0, 42, (byte) 149, (byte) 168, 30, 0, (byte) 137, (byte) 249, 102, (byte) 224, 32, 32, 32, 64};
            byte[] header_pps = {0, 0, 0, 1, 104, (byte) 206, 60, (byte) 128, 0, 0, 0, 1, 6, (byte) 229, 1, (byte) 151, (byte) 128};
            mediaformat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
            mediaformat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
        }
        //设置帧率
        mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        //https://developer.android.com/reference/android/media/MediaFormat.html#KEY_MAX_INPUT_SIZE
        //设置配置参数，参数介绍 ：
        // format   如果为解码器，此处表示输入数据的格式；如果为编码器，此处表示输出数据的格式。
        //surface   指定一个surface，可用作decode的输出渲染。
        //crypto    如果需要给媒体数据加密，此处指定一个crypto类.
        //   flags  如果正在配置的对象是用作编码器，此处加上CONFIGURE_FLAG_ENCODE 标签。
        mCodec.configure(mediaformat, mHolder.getSurface(), null, 0);
        mCodec.start();
        startDecodingThread();
    }

    private void startDecodingThread() {
        mDecodeThread = new Thread(new DecodeThread());
        mDecodeThread.start();
    }
}
