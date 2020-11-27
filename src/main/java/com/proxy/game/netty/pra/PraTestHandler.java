package com.proxy.game.netty.pra;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PraTestHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext in;

    public PraTestHandler(ChannelHandlerContext in) {
        this.in = in;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("PraTestHandler ctx {}",ctx);
        in.channel().writeAndFlush(msg);
        //in.pipeline().addLast(new HttpResponseEncoder());
    }
}
