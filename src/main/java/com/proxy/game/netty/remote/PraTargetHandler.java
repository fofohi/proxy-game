package com.proxy.game.netty.remote;


import com.proxy.game.netty.pojo.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class PraTargetHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext toLocalClient;

    public PraTargetHandler(ChannelHandlerContext toLocalClient) {
        this.toLocalClient = toLocalClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("PraTargetHandler {}",msg);
        if(msg instanceof FullHttpResponse){
            RemoteToLocalPojo remotePojo = new RemoteToLocalPojo();
            FullHttpResponse fMsg = (FullHttpResponse) msg;
            ByteBuf buf = fMsg.content();
            String content = buf.toString(CharsetUtil.UTF_8);
            remotePojo.setContent(content.getBytes());
            HashMap<String, String> headerMap = new HashMap<>();
            List<Map.Entry<String, String>> entries = fMsg.headers().entries();
            for (Map.Entry<String, String> entry : entries) {
                headerMap.put(entry.getKey(),entry.getValue());
            }
            remotePojo.setHeaders(headerMap);
            //toLocalClient.pipeline().addLast(new ToClientEncoder());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    fMsg.content()
            );
            for (Map.Entry<String, String> entry : entries) {
                response.headers().add(entry.getKey(),entry.getValue());
            }

            toLocalClient.pipeline().addLast(new HttpResponseEncoder());
            toLocalClient.pipeline().remove("remoteKryo1");
            //写回去
            toLocalClient.channel().writeAndFlush(response);
        }
    }
}
