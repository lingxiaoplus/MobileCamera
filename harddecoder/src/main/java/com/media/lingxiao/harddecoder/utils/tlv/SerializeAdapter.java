package com.media.lingxiao.harddecoder.utils.tlv;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import java.nio.ByteOrder;

public abstract class SerializeAdapter {
    protected int messageID;
    protected SerializeAdapter(int _messageID) {
        this.messageID = _messageID;
    }

    public String sendTo(IoSession session, int sysType, int majorProtocol, int minorProtocol) throws Exception {
        IoBuffer buffer = IoBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(Constants.createType(sysType, majorProtocol, minorProtocol, messageID));
        buffer.putInt(0);
        buffer.flip();
        session.write(buffer);
        return Constants.NOERROR;
    }
}
