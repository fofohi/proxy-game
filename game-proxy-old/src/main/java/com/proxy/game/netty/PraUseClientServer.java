package com.proxy.game.netty;

import com.proxy.game.client.OsHelper;
import com.proxy.game.netty.pojo.KryoMsgEncoder;
import com.proxy.game.netty.pra.PraHttpInboundHandler;
import com.proxy.game.netty.pra.PraHttpProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PraUseClientServer {

    public static ConcurrentHashMap<String,Object> map = new ConcurrentHashMap<>();



    public static void main(String[] args) {
        try {
            EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(2);
            EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(4);
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            //ch.pipeline().addLast("httpAggregator",new HttpObjectAggregator(1024000 << 2));
                            ch.pipeline().addLast("praHttpInbound1",new PraHttpInboundHandler());

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
