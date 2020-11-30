package com.proxy.game.netty.pojo;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ToClientEncoder extends MessageToByteEncoder<RemoteToLocalPojo> {
    //private KryoSerializer serializer = KryoSerializerFactory.getSerializer(RemotePojo.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, RemoteToLocalPojo msg, ByteBuf out) throws Exception {
        // 1. 将对象转换为byte
        byte[] body = toByteArray(msg);
        // 2. 读取消息的长度
        int dataLength = body.length;
        // 3. 先将消息长度写入，也就是消息头
        out.writeInt(dataLength);
        out.writeBytes(body);
    }

    public byte[] toByteArray (Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray ();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return bytes;
    }
}

