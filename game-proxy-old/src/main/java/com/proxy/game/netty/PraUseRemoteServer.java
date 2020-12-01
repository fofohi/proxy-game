package com.proxy.game.netty;

import com.proxy.game.client.OsHelper;
import com.proxy.game.netty.pojo.KryoMsgDecoder;
import com.proxy.game.netty.pojo.KryoMsgEncoder;
import com.proxy.game.netty.pra.PraHttpInboundHandler;
import com.proxy.game.netty.remote.PraUseRemoteHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class PraUseRemoteServer {

    public static void main(String[] args) {
        EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(1);
        EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(0);
        try {

            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast("remoteKryo1",new KryoMsgDecoder());
                            ch.pipeline().addLast("remoteHandler",new PraUseRemoteHandler());

                        }
                    })
            ;
            Channel httpChannel = b.bind(9077).sync().channel();
            httpChannel.closeFuture().sync();
        } catch (Exception e) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
    }
}
