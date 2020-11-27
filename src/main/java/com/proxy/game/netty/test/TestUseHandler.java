package com.proxy.game.netty.test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TestUseHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("TestUseHandler channelActive");
        super.channelActive(ctx);
        ctx.pipeline().read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("TestUseHandler");
        System.out.println("TestUseHandler Use msg is " + msg);
        //super.channelRead(ctx, msg);
        System.out.println("====>" + msg);
        ctx.read();
    }
}
