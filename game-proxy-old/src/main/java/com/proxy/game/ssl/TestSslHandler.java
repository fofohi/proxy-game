package com.proxy.game.ssl;

import com.proxy.game.netty.pra.PraHttpProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;

import java.util.ArrayList;

public class TestSslHandler extends ChannelInboundHandlerAdapter {

    private ArrayList<HttpContent> contents = new ArrayList<>();

    private HttpRequest request;

    private boolean sslOk = false;

    private Channel remoteChannel;

    private Channel clientChannel;

    @Override
    public void channelRead(ChannelHandlerContext browserAndServer, Object msg) throws Exception {
        clientChannel = browserAndServer.channel();

        if (sslOk) {
            remoteChannel.writeAndFlush(msg);
            return;
        }
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            return;
        } else {
            contents.add((HttpContent) msg);
            if (msg instanceof LastHttpContent) {
                if (request.method().equals(HttpMethod.CONNECT)) {
                    ChannelFuture sslFuture = clientChannel.writeAndFlush(Unpooled.wrappedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes()));
                    sslFuture.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess() && future.isDone()){
                            clientChannel.pipeline().remove(HttpRequestDecoder.class);
                            sslOk = true;
                        }
                    });
                }
            }
        }
        String hostAndPortString = request.headers().get("Host");
        String[] hostAndPort = hostAndPortString.split(":");
        Bootstrap b = new Bootstrap();
        b.group(clientChannel.eventLoop())
                .channel(clientChannel.getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TestSsl2Handler(clientChannel));
                    }
                });
        ChannelFuture f = b.connect(hostAndPort[0], hostAndPort.length > 1 ? Integer.parseInt(hostAndPort[1]) : 80);
        remoteChannel = f.channel();

        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                remoteChannel.writeAndFlush(request);
                contents.forEach(o -> remoteChannel.writeAndFlush(o));
            } else {
                clientChannel.close();
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
