package com.proxy.game.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseDecoder;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PraByteHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext browserToLocalServerChannel;

    public PraByteHandler(ChannelHandlerContext browserToLocalServerChannel) {
        this.browserToLocalServerChannel = browserToLocalServerChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        browserToLocalServerChannel.pipeline().addLast(new HttpResponseDecoder());
        browserToLocalServerChannel.pipeline().addLast(new HttpObjectAggregator(1024000));
        browserToLocalServerChannel.channel().writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
