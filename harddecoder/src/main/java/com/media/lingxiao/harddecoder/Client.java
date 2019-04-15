package com.media.lingxiao.harddecoder;

import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import com.media.lingxiao.harddecoder.decoder.H264Decoder;
import com.media.lingxiao.harddecoder.tlv.Constants;
import com.media.lingxiao.harddecoder.tlv.TLVCodecFactory;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;

import static com.media.lingxiao.harddecoder.tlv.Constants.MESSAGE_ID_STRAME;

public class Client{

    private NioSocketConnector streamConnection;
    private IoSession configSession, streamSession;
    private String mIp;
    private int mPort;
    private static final String TAG = Client.class.getSimpleName();
    private volatile State m_state = State.NotInitialize;
    public enum State{
        /**
         * new了，还未Initialize
         */
        NotInitialize,
        /**
         * 未连接(或第一次连接未成功)
         */
        NotConnected,
        /**
         * 重连中(第一次连接成功后网络异常，正在尝试重连)
         */
        ReConnecting,
        /**
         * 已断开(第一次连接成功后网络异常，已经断开)
         */
        Disconnect,
        /**
         * 已连接
         */
        Connected,
        /**
         * 已断开(手动操作)
         */
        Disconnected,
        /**
         * 已卸载
         */
        UnInitialized
    }

    public Client(String ip,int port){
        this.mIp = ip;
        this.mPort = port;
        TLVCodecFactory codecFactory = new TLVCodecFactory();
        ProtocolCodecFilter codecFilter = new ProtocolCodecFilter(codecFactory);
        streamConnection = new NioSocketConnector();
        streamConnection.getFilterChain().addLast("tlv", codecFilter);
        streamConnection.setHandler(new StreamClientHandler());
        streamConnection.getSessionConfig().setIdleTime(IdleStatus.WRITER_IDLE, 5); // 10秒未发出数据的话要发心跳
        streamConnection.addListener(new StreamAutoReconnectHandler());   //断线重连
        streamConnection.setConnectTimeoutMillis(5000);
        m_state = State.NotConnected;
    }

    public boolean play(SurfaceHolder holder,int w,int h){
        switch (m_state) {
            case NotInitialize: // 未初始化，返回false
                return false;
            case ReConnecting: // 正在重连，返回false
                return false;
            case Connected: // 已经连上，返回true
                return true;
            case UnInitialized: // 已经卸载，返回false
                return false;
            default:
                break;
        }
        streamConnection.setDefaultRemoteAddress(new InetSocketAddress(mIp, mPort));
        ConnectFuture future = streamConnection.connect();
        future.awaitUninterruptibly();
        if (!future.isConnected()){
            Log.i(TAG,"视频端口未连接:");
            return false;
        }
        streamSession = future.getSession();
        Log.i(TAG,"视频端口连接:" + streamSession);
        if (streamSession == null){
            m_state = State.ReConnecting; // 如果连接未成功，则设备进入自动重连状态
        }else {
            m_state = State.Connected;
        }
        H264Decoder.getInstance().play(holder,w,h);
        //createFilePath();
        return streamSession != null;
    }

    public void stopPlay(){
        H264Decoder.getInstance().stop();
        if(m_state != State.Connected){
            return;
        }
        try {
            streamSession.closeNow();
            m_state = State.UnInitialized;
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private BufferedOutputStream outputStream;
    private void createFilePath(){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/h264_from_mina.h264";
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    private class StreamClientHandler extends IoHandlerAdapter{
        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            //super.messageReceived(session, message);
            IoBuffer buffer = (IoBuffer) message;
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int type = buffer.getInt();
            int messageID = (type & 0x3FF);
            int len = buffer.getInt();
            Log.i(TAG,"messageID："+messageID+"  type: "+type);
            switch (messageID) {
                case MESSAGE_ID_STRAME:
                    if(buffer.remaining() < 4){
                        return;
                    }

                    int type1 = buffer.getInt();
                    if(type1 != 2){
                        return;
                    }
                    if(buffer.remaining() < 24){
                        return;
                    }
                    int width = buffer.getInt();
                    int height = buffer.getInt();
                    long seq_no0 = buffer.getUnsignedInt();
                    long seq_no1 = buffer.getUnsignedInt();
                    Log.i(TAG,
                            "width: "+width+" " +
                            " height: "+height+
                            "  seq_no0: "+seq_no0+
                            " seq_no1: "+seq_no1);
                    int bufferSize = buffer.getInt(); //视频帧大小
                    if(buffer.remaining() < bufferSize) {
                        return;
                    }
                    // 做个最大判定，不能太大了！
                    if(bufferSize > 1024 * 1024) {
                        return;
                    }
                    byte[] h264Segment = new byte[bufferSize];
                    buffer.get(h264Segment);
                    Log.i(TAG,"回调：" + h264Segment.length);
                    //outputStream.write(h264Segment);
                    if (mCameraDataCallback != null){
                        mCameraDataCallback.onH264DataFrame(h264Segment,width,height,seq_no0);
                    }
                    H264Decoder.getInstance().handleH264(h264Segment);
                    break;
            }
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            if(status == IdleStatus.WRITER_IDLE) {
                if(System.currentTimeMillis() - session.getLastWriteTime() >= 9000)
                    Constants.sendHeartBeat(session);
            } else if(status == IdleStatus.READER_IDLE) {
                long usecBetween = System.currentTimeMillis() - session.getLastReadTime();
                int secBetween = (int) (usecBetween / 1000);
                if(secBetween > 14) {
                    // 设备很长时间没发过来数据了
                    session.closeNow();
                }
            }
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            super.exceptionCaught(session, cause);
        }
    }

    /**
     * 配置连接断线重连
     */
    private final class StreamAutoReconnectHandler implements IoServiceListener {

        @Override
        public void serviceActivated(IoService ioService) throws Exception {

        }

        @Override
        public void serviceIdle(IoService ioService, IdleStatus idleStatus) throws Exception {

        }

        @Override
        public void serviceDeactivated(IoService ioService) throws Exception {

        }

        @Override
        public void sessionCreated(IoSession ioSession) throws Exception {

        }

        @Override
        public void sessionClosed(IoSession ioSession) throws Exception {

        }

        @Override
        public void sessionDestroyed(IoSession ioSession) throws Exception {
            // 需要重连
            while(m_state == State.Connected || m_state == State.ReConnecting) {
                try {
                    ConnectFuture future = streamConnection.connect();
                    future.awaitUninterruptibly();// 等待连接创建成功
                    streamSession = future.getSession();// 获取会话
                    if(m_state != State.Connected && m_state != State.ReConnecting) {
                        Log.i(TAG,"不需要断线重连");
                        streamSession.closeNow();
                        break;
                    }
                    if (streamSession != null) {
                        m_state = State.Connected;
                        break;
                    }
                    Log.i(TAG,"断线重连");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    private CameraDataCallback mCameraDataCallback;

    public void setCameraDataCallback(CameraDataCallback cameraCallback) {
        this.mCameraDataCallback = cameraCallback;
    }

    public interface CameraDataCallback {
        void onH264DataFrame(byte[] h264, int width, int height,long seq_no);
    }
}
