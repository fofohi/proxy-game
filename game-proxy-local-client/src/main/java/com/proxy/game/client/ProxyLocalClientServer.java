package com.proxy.game.client;

import com.proxy.game.client.handler.ProxyBrowserToLocalInHandler;
import com.proxy.game.pojo.context.HandlerContext;
import com.proxy.game.pojo.util.OsHelper;
import com.proxy.game.pojo.util.SocksServerUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * 本地链接用的服务器
 */
public class ProxyLocalClientServer {

    public static void main(String[] args) {
        ProxyLocalClientServer instance = new ProxyLocalClientServer();
        instance.serverStart(9078);
    }

    public void serverStart(int port){
        Channel httpChannel = null;
        try {
            EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(1);
            EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(4);

            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(10240000));
                            ch.pipeline().addLast(HandlerContext.PROXY_BROWSER_TO_LOCAL_IN_HANDLER,new ProxyBrowserToLocalInHandler());
                        }
                    })
            ;
            httpChannel = b.bind(port).sync().channel();
            httpChannel.closeFuture().sync();
        } catch (Exception e) {
            if(httpChannel != null){
                SocksServerUtils.closeOnFlush(httpChannel);
            }
        }finally {
            if(httpChannel == null){
                this.serverStart(9078);
            }
        }
    }
}
