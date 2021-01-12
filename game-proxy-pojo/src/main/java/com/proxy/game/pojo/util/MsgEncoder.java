package com.proxy.game.pojo.util;

import com.alibaba.fastjson.JSON;
import com.proxy.game.pojo.RemotePojo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MsgEncoder extends MessageToByteEncoder<RemotePojo> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotePojo msg, ByteBuf out) {
        //byte[] body = ProtostuffUtils.serialize(JSON.toJSONString(msg));
        byte[] body = JSON.toJSONString(msg).getBytes();
        int dataLength = body.length;
        out.writeInt(dataLength);
        out.writeBytes(body);
    }
}
