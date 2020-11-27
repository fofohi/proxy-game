package com.proxy.game.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

public class HttpSnoopClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ChannelHandlerContext channelHandlerContext;

    private static String contents = "";


    HttpSnoopClientHandler(ChannelHandlerContext clientCtx){
         this.channelHandlerContext = clientCtx;
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            System.err.println("STATUS: " + response.status());
            System.err.println("VERSION: " + response.protocolVersion());
            System.err.println();

            if (!response.headers().isEmpty()) {
                for (CharSequence name: response.headers().names()) {
                    for (CharSequence value: response.headers().getAll(name)) {
                        System.err.println("HEADER: " + name + " = " + value);
                    }
                }
                System.err.println();
            }

            if (HttpUtil.isTransferEncodingChunked(response)) {
                System.err.println("CHUNKED CONTENT {");
            } else {
                System.err.println("CONTENT {");
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            HttpSnoopClientHandler.contents += content.content().toString(CharsetUtil.UTF_8);
            System.err.print(contents);
            System.err.flush();
            if (content instanceof LastHttpContent) {
                System.err.println("} END OF CONTENT");
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(contents.getBytes()));

                for (CharSequence name: response.headers().names()) {
                    for (CharSequence value: response.headers().getAll(name)) {
                        response.headers().set(name,value);
                    }
                }

                channelHandlerContext.writeAndFlush(response);
                contents = "";
                ReferenceCountUtil.release(contents);
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
