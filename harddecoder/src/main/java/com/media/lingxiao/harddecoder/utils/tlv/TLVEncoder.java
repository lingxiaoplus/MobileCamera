package com.media.lingxiao.harddecoder.utils.tlv;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Created by 肖何 on 2017/12/8.
 */

public final class TLVEncoder extends ProtocolEncoderAdapter {
    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        out.write(message);
        out.flush();
    }
}
