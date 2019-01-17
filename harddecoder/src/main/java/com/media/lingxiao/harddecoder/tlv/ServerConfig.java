package com.media.lingxiao.harddecoder.tlv;

public class ServerConfig {
    /**
     * 数据封包中系统类型
     */
    public int sysType = 7;
    /**
     * 数据封包中主版本号
     */
    public int majorProtocol = 2;
    /**
     * 数据封包中次版本号
     */
    public int minorProtocol = 0;

    /**
     * 多长时间未发送过数据后需要发送心跳
     * 小于0表示不发送心跳
     */
    public int heartBeatInterval = 5;
}
