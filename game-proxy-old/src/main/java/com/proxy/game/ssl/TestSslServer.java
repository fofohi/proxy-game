package com.proxy.game.ssl;

import com.proxy.game.client.OsHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TestSslServer {

    public static void main(String[] args) {
        try {
            EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(1);
            EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(0);
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast(new TestSslHandler());
                        }
                    })
            ;
            Channel httpChannel = b.bind(9078).sync().channel();
            httpChannel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
