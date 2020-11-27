package com.proxy.game.netty.test;

import com.proxy.game.client.OsHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;

public class TestUseServer {


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
                            ch.pipeline().addLast("test1",new TestUseHandler());
                            ch.pipeline().addLast("test2",new TestUseTwoHandler());
                            ch.pipeline().addFirst("test3",new TestUseOutHandler());
                            ch.pipeline().addFirst("test4",new TestUseOutTwoHandler());
                            /**
                             *     test4 out find pre so test4 is first then find pre is default
                             *     then skip to test3 find it's pre is test4 then invoke test4
                             *
                             *     default find default's next test1, then run test1,
                             *      ` then find test1's next test2,then run test2
                             *
                             *     outbound when add first a then b , run as a then b
                             *              when add last a then b , run as b then a
                             *     inbound when add first a then b , run as b then a
                             *             when add last a then b , run as a then b
                             *     if use fits logic
                             *     use inbound add last
                             *     use outbound add first as wish
                             *
                             *     outbound is find pre
                             *     inbound is find next
                             */

                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false)
            ;
            Channel httpChannel = b.bind(9078).sync().channel();
            httpChannel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
