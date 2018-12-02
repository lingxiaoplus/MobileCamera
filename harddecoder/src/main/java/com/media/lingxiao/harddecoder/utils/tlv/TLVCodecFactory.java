package com.media.lingxiao.harddecoder.utils.tlv;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * Created by 肖何 on 2017/12/8.
 */

public final class TLVCodecFactory implements ProtocolCodecFactory {
    private TLVDecoder decoder;
    private TLVEncoder encoder;

    public TLVCodecFactory() {
        decoder = new TLVDecoder();
        encoder = new TLVEncoder();
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return decoder;
    }
}
