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
                            //ch.pipeline().addLast("httpCoder",new HttpServerCodec());
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast("httpAggregator",new HttpObjectAggregator(1024000 << 2));
                            ch.pipeline().addLast("praHttpInbound1",new PraHttpInboundHandler());
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
                             *
                             *
                             *      server 负责进行request 解码and response编码
                             *      HttpServerCodec 里面组合了HttpResponseEncoder和HttpRequestDecoder
                             *
                             *      cline 负责进行request变码 and response 解码
                             *      HttpClientCodec 里面组合了HttpRequestEncoder和HttpResponseDecoder
                             *
                             *
                             *
                             *
                             */

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
