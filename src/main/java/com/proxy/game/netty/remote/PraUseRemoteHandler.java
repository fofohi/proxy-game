package com.proxy.game.netty.remote;

import com.alibaba.fastjson.JSON;
import com.proxy.game.netty.pojo.RemotePojo;
import com.proxy.game.netty.pra.PraHttpProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PraUseRemoteHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        RemotePojo pojo = JSON.parseObject((String) msg, RemotePojo.class);
        final Bootstrap b2 = new Bootstrap();
        b2.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpRequestEncoder());
                        ch.pipeline().addLast(new HttpResponseDecoder());
                        ch.pipeline().addLast(new HttpObjectAggregator(102400000));
                        ch.pipeline().addLast(new PraTargetHandler());
                        //ch.pipeline().addLast(new HttpRequestEncoder());
                        //ch.pipeline().addLast(new RelayHandler(browserToLocalServerChannel.channel()));
                    }
                })
        ;
        b2.connect("47.101.39.121",30603).addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()){
                if(pojo.getUri().contains("443")){
                    return;
                }
                log.info("in here {}",future.channel());
                DefaultFullHttpRequest full = new DefaultFullHttpRequest(
                        HttpVersion.HTTP_1_1,
                        new HttpMethod(pojo.getMethod()),
                        "/assets_en/1606182644/js_low/config.js", Unpooled.wrappedBuffer(pojo.getContent().getBytes()
                )
                );
                for (Map.Entry<String, String> stringStringEntry : pojo.getHeaders().entrySet()) {
                    full.headers().add(stringStringEntry.getKey(),stringStringEntry.getValue());
                }
                future.channel().writeAndFlush(full);
            }
        });




        super.channelRead(ctx, msg);
    }

}
