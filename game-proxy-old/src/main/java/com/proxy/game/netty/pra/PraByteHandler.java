package com.proxy.game.netty.pra;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PraByteHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext browserToLocalServerChannel;

    public PraByteHandler(ChannelHandlerContext browserToLocalServerChannel) {
        this.browserToLocalServerChannel = browserToLocalServerChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("msg {}",msg);
        browserToLocalServerChannel.pipeline().addLast(new HttpResponseDecoder());
        browserToLocalServerChannel.pipeline().addLast(new HttpObjectAggregator(10240000));
        browserToLocalServerChannel.channel().writeAndFlush(msg);
    }
}
