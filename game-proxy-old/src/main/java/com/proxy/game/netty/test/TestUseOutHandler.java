package com.proxy.game.netty.test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;


public class TestUseOutHandler extends ChannelOutboundHandlerAdapter {


    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        System.out.println("TestUseOutHandler");
        //ctx.read();
    }
}
