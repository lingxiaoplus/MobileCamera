package com.media.lingxiao.harddecoder.tlv;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import java.nio.ByteOrder;

public class Constants {
    public static final String NOERROR = "no error";

    public static final int MESSAGE_ID_HEARTBEAT = 2; // 心跳包
    public static final int MESSAGE_ID_ACK = 4; // 响应包
    public static final int MESSAGE_ID_REGIST = 111; // 数据包
    public static final int MESSAGE_ID_DETECTRESULT = 112; // 检测对比结果
    public static final int MESSAGE_ID_STRAME = 103; //视频流

    public static final int PORT = 5567;
    public static final int PORT_DATA = 2233;

    private static final int TYPE_EMPTY = 201344000; // 空的messageId

    /**
     * 从系统类型、主版本号、次版本号以及消息类型创建Type字段
     * @param sysType 系统类型
     * @param majorProtocol 主版本号
     * @param minorProtocol 次版本号
     * @param msgType 消息类型
     * @return Type
     */
    public static int createType(int sysType, int majorProtocol, int minorProtocol, int msgType) {
        int type = 0;

        type |= (sysType << 24);
        type |= (majorProtocol << 14);
        type |= (minorProtocol << 10);
        type |= msgType;

        return type;
    }
    // 从MessageID创建TYPE
    public static int createType(int messageID) {
        return createType(TYPE_EMPTY, messageID);
    }

    // 从请求包TYPE字段创建回应包请求字段
    public static int createType(int originType, int messageID) {
        return (originType & -1024) | messageID;
    }

    // 发送心跳包
    public static void sendHeartBeat(IoSession session) {
        IoBuffer buffer = IoBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int type = createType(MESSAGE_ID_HEARTBEAT);
        buffer.putInt(type);
        buffer.putInt(0);
        buffer.flip();
        session.write(buffer);
    }

}
