package com.media.lingxiao.harddecoder.utils.tlv;

/**
 * @author lingxiao 一般用于发送心跳包等
 */
public class DummySerializable extends SerializeAdapter{
    public DummySerializable(int _messageID) {
        super(_messageID);
    }
}
