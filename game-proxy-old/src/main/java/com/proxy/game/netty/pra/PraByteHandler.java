package com.proxy.game.netty.pra;

import com.proxy.game.netty.pojo.RemoteToLocalPojo;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Set;

@Slf4j
public class PraByteHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext browserToLocalServerChannel;

    public PraByteHandler(ChannelHandlerContext browserToLocalServerChannel) {
        this.browserToLocalServerChannel = browserToLocalServerChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        log.info("PraByteHandler msg {}",msg);
        /*RemoteToLocalPojo remoteToLocalPojo = (RemoteToLocalPojo) msg;
        browserToLocalServerChannel.pipeline().addLast(new HttpResponseEncoder());
        String contentString = new String(remoteToLocalPojo.getContent());
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(contentString, CharsetUtil.UTF_8)
                );
        Set<Map.Entry<String, String>> entries = remoteToLocalPojo.getHeaders().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            response.headers().add(entry.getKey(),entry.getValue());
        }*/
        browserToLocalServerChannel.pipeline().addLast(new HttpResponseDecoder());
        browserToLocalServerChannel.pipeline().addLast(new HttpObjectAggregator(10000000));
        browserToLocalServerChannel.channel().writeAndFlush(msg);
    }

    /**
     * 数组转对象
     * @param bytes
     * @return
     */
    public Object toObject (byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream (bytes);
            ObjectInputStream ois = new ObjectInputStream (bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return obj;
    }
}
