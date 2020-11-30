package com.proxy.game.netty;

import com.proxy.game.client.OsHelper;
import com.proxy.game.netty.pojo.KryoMsgDecoder;
import com.proxy.game.netty.pojo.KryoMsgEncoder;
import com.proxy.game.netty.pra.PraHttpInboundHandler;
import com.proxy.game.netty.test.TestUseHandler;
import com.proxy.game.netty.test.TestUseOutHandler;
import com.proxy.game.netty.test.TestUseOutTwoHandler;
import com.proxy.game.netty.test.TestUseTwoHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;

public class PraUseClientServer {

    public static void main(String[] args) {
        try {
            EventLoopGroup bossGroup = OsHelper.buildEventLoopGroup(1);
            EventLoopGroup workerGroup = OsHelper.buildEventLoopGroup(0);
            // Configure the server.
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 10240);
            b.group(bossGroup, workerGroup)
                    .channel(OsHelper.serverSocketChannelClazz())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast("httpAggregator",new HttpObjectAggregator(1024000 << 2));
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
