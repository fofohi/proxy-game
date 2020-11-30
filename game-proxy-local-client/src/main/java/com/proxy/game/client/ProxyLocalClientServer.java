package com.proxy.game.client;

import com.proxy.game.client.handler.ProxyBrowserToLocalInHandler;
import com.proxy.game.pojo.util.OsHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;

/**
 * 本地链接用的服务器
 */
public class ProxyLocalClientServer {

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
                            ch.pipeline().addLast("proxyBrowserToLocalRequestDecoder",new HttpRequestDecoder());
                            ch.pipeline().addLast("httpAggregator",new HttpObjectAggregator(1024000 << 2));
                            ch.pipeline().addLast("proxyBrowserToLocalInHandler",new ProxyBrowserToLocalInHandler());
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
