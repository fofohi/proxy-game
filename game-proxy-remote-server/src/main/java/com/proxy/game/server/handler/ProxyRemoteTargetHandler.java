package com.proxy.game.server.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ProxyRemoteTargetHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext toLocalClient;

    public ProxyRemoteTargetHandler(ChannelHandlerContext toLocalClient) {
        this.toLocalClient = toLocalClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof FullHttpResponse){
            FullHttpResponse fMsg = (FullHttpResponse) msg;
            List<Map.Entry<String, String>> entries = fMsg.headers().entries();

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    fMsg.status(),
                    fMsg.content()
            );

            for (Map.Entry<String, String> entry : entries) {
                response.headers().add(entry.getKey(),entry.getValue());
            }
            toLocalClient.pipeline().addLast(new HttpResponseEncoder());
            //写回去
            toLocalClient.channel().writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

}
