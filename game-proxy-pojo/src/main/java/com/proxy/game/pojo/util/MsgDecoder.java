package com.proxy.game.pojo.util;

import com.alibaba.fastjson.JSON;
import com.proxy.game.pojo.RemotePojo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MsgDecoder extends ByteToMessageDecoder {
    private static final int HEAD_LENGTH = 4;
    //a 7
    //d 9
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HEAD_LENGTH) {
            return;
        }
        //
        in.markReaderIndex();

        int dataLength = in.readInt();
        if (dataLength <= 0) {
            ctx.close();
        }
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        // 至此，读取到一条完整报文
        byte[] body = new byte[dataLength];
        in.readBytes(body);
        // 将bytes数组转换为我们需要的对象
        String s = ProtostuffUtils.deserialize(body, String.class);
        out.add(JSON.parseObject(s,RemotePojo.class));
    }


}
