package com.proxy.game.client.handler;

import com.proxy.game.pojo.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxySslHandler extends ChannelInboundHandlerAdapter {

    private volatile boolean sslOk = false;

    private Channel remoteChannel ;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (sslOk) {
            remoteChannel.writeAndFlush(msg);
            return;
        }
        final FullHttpRequest fullMsg = (FullHttpRequest) msg;
        Channel clientChannel = ctx.channel();
        clientChannel.config().setAutoRead(false);
        ChannelFuture sslFuture = clientChannel.writeAndFlush(Unpooled.wrappedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes()));
        sslFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess() && future.isDone()){
                ctx.pipeline().remove(HttpRequestDecoder.class);
                ctx.pipeline().remove(ProxyBrowserToLocalInHandler.class);
                sslOk = true;
            }
        });

        String hostAndPortString = fullMsg.headers().get("Host");
        String[] hostAndPort = hostAndPortString.split(":");
        Bootstrap b = new Bootstrap();
        b.group(clientChannel.eventLoop())
                .channel(clientChannel.getClass())
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new ProxySslRelayHandler(clientChannel));
                    }
                });
        ChannelFuture f = b.connect(hostAndPort[0], hostAndPort.length > 1 ? Integer.parseInt(hostAndPort[1]) : 80);
        remoteChannel = f.channel();

        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                clientChannel.config().setAutoRead(true);
                remoteChannel.pipeline().addLast(new ProxyToClientByteHandler(ctx));
                remoteChannel.writeAndFlush(msg);
            } else {
                SocksServerUtils.closeOnFlush(clientChannel);
                SocksServerUtils.closeOnFlush(remoteChannel);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("error {}",cause.getMessage());
    }
}
