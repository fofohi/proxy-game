package com.proxy.game.netty.pra;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;


@Slf4j
public class PraHttpInboundHandlerBak extends ChannelInboundHandlerAdapter {

    private ArrayList<HttpContent> contents = new ArrayList<>();

    private HttpRequest request;

    @Override
    public void channelRead(ChannelHandlerContext browserToLocalServerChannel, Object msg){
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
        }else{
            contents.add((HttpContent) msg);
            if(msg instanceof LastHttpContent){
                Promise<Channel> promise = browserToLocalServerChannel.executor().newPromise();
                promise.addListener(
                        (FutureListener<Channel>) future -> {
                            //当前客户端对iplc的链接
                            Channel localServerToIplcChannel = future.getNow();
                            if (future.isSuccess()) {

                                RelayHandler clientEndToRemoteHandler = new RelayHandler(localServerToIplcChannel);

                                clientEndToRemoteHandler.channelRead(browserToLocalServerChannel, request);
                                contents.forEach(content -> {
                                    try {
                                        clientEndToRemoteHandler.channelRead(browserToLocalServerChannel, content);
                                    } catch (Exception e) {
                                        log.error("处理非CONNECT方法的代理请求失败！", e);
                                    }
                                });

                                browserToLocalServerChannel.pipeline().remove(PraHttpInboundHandlerBak.class);
                                browserToLocalServerChannel.pipeline().remove(HttpResponseEncoder.class);
                                browserToLocalServerChannel.pipeline().addLast(clientEndToRemoteHandler);

                            } else {
                            /*
                            500
                            ctx.channel().writeAndFlush(
                                    new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                            );*/
                                SocksServerUtils.closeOnFlush(browserToLocalServerChannel.channel());
                            }
                        });
                final Bootstrap b = new Bootstrap();
                //localServerToIplcChannel
                b.group(browserToLocalServerChannel.channel().eventLoop())
                        .channel(browserToLocalServerChannel.channel().getClass())
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new PraHttpProxyHandler(promise));
                                //ch.pipeline().addLast(new HttpRequestEncoder());
                                //ch.pipeline().addLast(new RelayHandler(browserToLocalServerChannel.channel()));
                            }
                        })
                ;
                b.connect("47.101.39.121",30603).addListener((ChannelFutureListener) future -> {
                });
            }
        }
    }
}
