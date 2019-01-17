package com.media.lingxiao.harddecoder;

import android.util.Log;

import com.media.lingxiao.harddecoder.events.StrameEventHandler;
import com.media.lingxiao.harddecoder.model.VideoStreamModel;
import com.media.lingxiao.harddecoder.pkgs.PreViewFrameResponse;
import com.media.lingxiao.harddecoder.tlv.Constants;
import com.media.lingxiao.harddecoder.tlv.DummySerializable;
import com.media.lingxiao.harddecoder.tlv.ServerConfig;
import com.media.lingxiao.harddecoder.tlv.TLVCodecFactory;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;

public class Server{
    //视频注册
    private StrameEventHandler mStrameEventHandler;
    private static final String TAG = Server.class.getSimpleName();
    private NioSocketAcceptor acceptor;
    private ServerConfig config;
    public Server() {
        acceptor = new NioSocketAcceptor();
        acceptor.setHandler(new StreamHandler());
        acceptor.getFilterChain().addLast("tlv",
                new ProtocolCodecFilter(new TLVCodecFactory()));
    }

    /**
     * 启动服务器监听
     *
     * @param port 要监听的端口号
     *
     * @return 是否成功
     */
    public boolean start(int port, ServerConfig _config) {
        this.config = _config;
        if(config.heartBeatInterval > 0) {
            // 发送空闲需要加心跳
            acceptor.getSessionConfig().setIdleTime(IdleStatus.WRITER_IDLE, config.heartBeatInterval);
            // 接收空闲判断连接是否已断开了
            acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, config.heartBeatInterval);
        }

        try {
            acceptor.setReuseAddress(true);
            acceptor.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /**
     * 停止服务器监听
     */
    public void stop() {
        acceptor.dispose(true);
    }

    public void broadcastPreviewFrameData(VideoStreamModel model){
        for (IoSession session: acceptor.getManagedSessions().values()) {
            PreViewFrameResponse frameResponse = null;
            Log.i(TAG,"视频数据发送1");
            try {
                frameResponse = new PreViewFrameResponse();
                frameResponse.setFrameData(model).sendTo(session);
            }catch (Exception e){
                Log.i(TAG,"视频数据发送失败");
                e.printStackTrace();
            }
        }
    }

    private class StreamHandler extends IoHandlerAdapter{
        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            super.messageReceived(session, message);
            IoBuffer buffer = (IoBuffer) message;
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int type = buffer.getInt();
            int messageID = (type & 0x3FF);
            int len = buffer.getInt();
            if(messageID == Constants.MESSAGE_ID_HEARTBEAT) {
                return;
            }
            if(messageID == Constants.MESSAGE_ID_ACK) {
                if(len < 8) return;
                int respMessageID = buffer.getInt();
                int ackCode = buffer.getInt();
                switch(respMessageID) {

                }
            }

            //如果是视频流
            if (messageID == Constants.MESSAGE_ID_STRAME){

            }

        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {

        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {

        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            // TODO 可能要定时判断是否该断开视频，避免带宽泄漏
            if(status == IdleStatus.WRITER_IDLE) {
                try {
                    sendDummy(session, Constants.MESSAGE_ID_HEARTBEAT);
                } catch (Throwable e) {
                    e.fillInStackTrace();
                }
            }
    	/*else if(status == IdleStatus.READER_IDLE) {
    		long usecBetween = Calendar.getInstance().getTime().getTime() - session.getLastReadTime();
    		int secBetween = (int) (usecBetween / 1000);
    		if(secBetween > 3 * config.heartBeatInterval) {
    			// 设备很长时间没发过来数据了
    			session.closeOnFlush();
    			return;
    		}
    	}*/
        }
    }


    private void sendDummy(IoSession session, int messsageID) throws Exception {
        new DummySerializable(messsageID).sendTo(session, config.sysType, config.majorProtocol, config.minorProtocol);
    }

    private void sendAck(IoSession session, int messageID, int ackCode) {
        IoBuffer buffer = IoBuffer.allocate(16); //8
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(Constants.createType(config.sysType, config.majorProtocol, config.minorProtocol, Constants.MESSAGE_ID_ACK)); //201
        buffer.putInt(8);  //0
        buffer.putInt(messageID);
        buffer.putInt(ackCode);
        Log.i(TAG,"ackCode:" + ackCode);
        buffer.flip();
        session.write(buffer);
    }
}
