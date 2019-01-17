package com.media.lingxiao.harddecoder.pkgs;


import android.util.Log;

import com.media.lingxiao.harddecoder.model.VideoStreamModel;
import com.media.lingxiao.harddecoder.tlv.Constants;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import java.nio.ByteOrder;

/**
 * Created by 任梦林 on 2018/5/23.
 */

public class PreViewFrameResponse {
    private VideoStreamModel streamModel;
    private static final String TAG = PreViewFrameResponse.class.getSimpleName();
    public PreViewFrameResponse setFrameData(VideoStreamModel model){
        this.streamModel = model;
        return this;
    }
    public String sendTo(IoSession session) throws Exception {
        if (null == streamModel){
            return "data is null!";
        }
        byte[] data = streamModel.getVideo();

        IoBuffer buffer = IoBuffer.allocate(8 + 24 +data.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(Constants.createType(Constants.MESSAGE_ID_STRAME));
        buffer.putInt(24 + data.length);

        buffer.putInt(streamModel.getType());
        buffer.putInt(streamModel.getWidth());
        buffer.putInt(streamModel.getHeight());
        buffer.putUnsignedInt(streamModel.getSeq_no0());
        buffer.putUnsignedInt(streamModel.getSeq_no1());

        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        session.write(buffer);
        Log.i(TAG,"buffer的limit："+buffer.limit()+"  len: "+(24 + data.length)
        +"messageID: "+Constants.createType(Constants.MESSAGE_ID_STRAME));
        return null;
    }
}
