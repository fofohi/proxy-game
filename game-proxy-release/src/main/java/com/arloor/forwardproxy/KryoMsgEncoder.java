package com.arloor.forwardproxy;

import com.alibaba.fastjson.JSON;
import com.arloor.forwardproxy.vo.RemotePojo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class KryoMsgEncoder extends MessageToByteEncoder<RemotePojo> {
    //private KryoSerializer serializer = KryoSerializerFactory.getSerializer(RemotePojo.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotePojo msg, ByteBuf out) throws Exception {
        // 1. 将对象转换为byte
        byte[] body = JSON.toJSONString(msg).getBytes();
        // 2. 读取消息的长度
        int dataLength = body.length;
        // 3. 先将消息长度写入，也就是消息头
        out.writeInt(dataLength);
        out.writeBytes(body);
    }
}
