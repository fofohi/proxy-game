package com.proxy.game.netty.test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TestUseTwoHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
       System.out.println("TestUseTwoHandler");
       // super.channelRead(ctx, msg);
    }
}
