package com.arloor.forwardproxy;

import com.alibaba.fastjson.JSON;
import com.arloor.forwardproxy.util.OsHelper;
import com.arloor.forwardproxy.util.SocksServerUtils;
import com.arloor.forwardproxy.vo.RemotePojo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;


@Slf4j
public class HttpProxyConnect2Handler extends ChannelInboundHandlerAdapter {


    private final Bootstrap b = new Bootstrap();

    private String host;
    private Integer port;
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        log.info("HttpProxyConnect2Handler {}" , msg);

        RemotePojo pojo = JSON.parseObject((String)msg, RemotePojo.class);


        Promise<Channel> promise = ctx.executor().newPromise();

        promise.addListener(
                (FutureListener<Channel>) future -> {
                    final Channel outboundChannel = future.getNow();
                    if (future.isSuccess()) {
                        //ctx本地和服务器的连接
                        ctx.pipeline().remove(HttpProxyConnect2Handler.this);

                        //服务器和目标服务器的连接
                        outboundChannel.pipeline().addLast(new HttpRequestEncoder());
                        outboundChannel.pipeline().addLast(new Relay2Handler(ctx.channel()));

                        //ctx ===> 目标服务器
                        //需要 这里拼装http请求
                        DefaultHttpRequest full = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                                new HttpMethod(pojo.getM()),
                                pojo.getU().replace("http://" + pojo.getHe().get("Host"),""));
                        for (Map.Entry<String, String> stringStringEntry : pojo.getHe().entrySet()) {
                            full.headers().add(stringStringEntry.getKey(),stringStringEntry.getValue());
                        }
                        outboundChannel.writeAndFlush(full);
                        for (byte[] s : pojo.getC()) {
                            outboundChannel.writeAndFlush(s);
                        }
                        /*Relay2Handler clientEndtoRemoteHandler = new Relay2Handler(outboundChannel);
                        ctx.pipeline().addLast(clientEndtoRemoteHandler);

                        DefaultHttpRequest full = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                                new HttpMethod(pojo.getM()),
                                pojo.getU().replace("http://" + pojo.getHe().get("Host"),""));
                        for (Map.Entry<String, String> stringStringEntry : pojo.getHe().entrySet()) {
                            full.headers().add(stringStringEntry.getKey(),stringStringEntry.getValue());
                        }
                        clientEndtoRemoteHandler.channelRead(ctx, full);
                        contents.forEach(content -> {
                            try {
                                clientEndtoRemoteHandler.channelRead(ctx, content);
                            } catch (Exception e) {
                                log.error("处理非CONNECT方法的代理请求失败！", e);
                            }
                        });*/
                    } else {
                        ctx.channel().writeAndFlush(
                                new DefaultHttpResponse(HttpVersion.HTTP_1_1, INTERNAL_SERVER_ERROR)
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
        String hostAndPort = pojo.getHe().get("Host");

        String[] hostAndPortString = hostAndPort.split(":");
        host = hostAndPortString[0];
        port = hostAndPortString.length > 1 ? Integer.parseInt(hostAndPortString[1]) : 80;
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
