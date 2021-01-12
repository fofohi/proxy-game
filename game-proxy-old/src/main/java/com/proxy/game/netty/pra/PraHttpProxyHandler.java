package com.proxy.game.netty.pra;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PraHttpProxyHandler extends ChannelInboundHandlerAdapter {

    private final Promise<Channel> promise;


    public PraHttpProxyHandler(Promise<Channel> promise) {
        this.promise = promise;
    }

    @Override
    public void channelActive(ChannelHandlerContext localServerToIplcChannel){
        localServerToIplcChannel.pipeline().remove(this);
        promise.setSuccess(localServerToIplcChannel.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
