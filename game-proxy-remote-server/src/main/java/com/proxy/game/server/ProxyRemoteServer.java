package com.proxy.game.server;

import com.proxy.game.pojo.context.HandlerContext;
import com.proxy.game.pojo.util.MsgDecoder;
import com.proxy.game.pojo.util.OsHelper;
import com.proxy.game.pojo.util.SocksServerUtils;
import com.proxy.game.server.handler.ProxyRemoteToLocalHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyRemoteServer {

    public static void main(String[] args) {
        Channel httpChannel = null;

        try {
            EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(1);
            EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(0);
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(HandlerContext.PROXY_MSG_DECODER,new MsgDecoder());
                            ch.pipeline().addLast(HandlerContext.PROXY_REMOTE_TO_LOCAL_HANDLER,new ProxyRemoteToLocalHandler());
                        }
                    })
            ;
            httpChannel = b.bind(9077).sync().channel();
            httpChannel.closeFuture().sync();
        } catch (Exception e) {
            if(httpChannel != null){
                SocksServerUtils.closeOnFlush(httpChannel);
            }
            log.error("error {}",e.getMessage());
        }
    }
}
