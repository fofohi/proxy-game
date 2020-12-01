package com.proxy.game.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface RequestHandler {
    Object handle(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx);
}
