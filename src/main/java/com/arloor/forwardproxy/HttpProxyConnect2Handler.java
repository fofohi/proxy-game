package com.arloor.forwardproxy;

import com.arloor.forwardproxy.util.OsHelper;
import com.arloor.forwardproxy.util.SocksServerUtils;
import com.arloor.forwardproxy.vo.Config;
import com.arloor.forwardproxy.vo.RemotePojo;
import com.arloor.forwardproxy.web.Dispatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;


@Slf4j
public class HttpProxyConnect2Handler extends ChannelInboundHandlerAdapter {


    private final Bootstrap b = new Bootstrap();

    private String host;
    private Integer port;
    private ArrayList<HttpContent> contents = new ArrayList<>();
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        log.info("HttpProxyConnect2Handler {}" , msg);

        RemotePojo remotePojo =  (RemotePojo) msg;


        Promise<Channel> promise = ctx.executor().newPromise();

        promise.addListener(
                (FutureListener<Channel>) future -> {
                    final Channel outboundChannel = future.getNow();
                    if (future.isSuccess()) {
                        ctx.pipeline().remove(HttpProxyConnect2Handler.this);
                        ctx.pipeline().remove(HttpResponseEncoder.class);
                        outboundChannel.pipeline().addLast(new HttpRequestEncoder());
                        outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                        RelayHandler clientEndtoRemoteHandler = new RelayHandler(outboundChannel);
                        ctx.pipeline().addLast(clientEndtoRemoteHandler);

                        clientEndtoRemoteHandler.channelRead(ctx, request);
                        contents.forEach(content -> {
                            try {
                                clientEndtoRemoteHandler.channelRead(ctx, content);
                            } catch (Exception e) {
                                log.error("处理非CONNECT方法的代理请求失败！", e);
                            }
                        });
                    } else {
                        ctx.channel().writeAndFlush(
                                new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                        );
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });
        // 4.连接目标网站
        final Channel inboundChannel = ctx.channel();
        b.group(inboundChannel.eventLoop())
                .channel(OsHelper.socketChannelClazz())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new DirectClientHandler(promise));
        b.connect(host,port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("connect host {} port {}",host,port);
            } else {
                ctx.channel().writeAndFlush(
                        new DefaultHttpResponse(HttpVersion.HTTP_1_1, INTERNAL_SERVER_ERROR)
                );
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
        });
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
