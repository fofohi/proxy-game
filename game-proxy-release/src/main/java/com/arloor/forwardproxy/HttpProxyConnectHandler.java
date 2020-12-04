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

@Slf4j
public class HttpProxyConnectHandler extends ChannelInboundHandlerAdapter {


    private final Bootstrap b = new Bootstrap();

    private String host;
    private int port;
    private HttpRequest request;
    private ArrayList<HttpContent> contents = new ArrayList<>();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            setHostPort(ctx);
        } else {
            //SimpleChannelInboundHandler会将HttpContent中的bytebuf Release，但是这个还会转给relayHandler，所以需要在这里预先retain
            ((HttpContent) msg).content().retain();
            contents.add((HttpContent) msg);
            //一个完整的Http请求被收到，开始处理该请求
            if (msg instanceof LastHttpContent) {

                if (request.uri().startsWith("/")) {
                    Dispatcher.handle(request, ctx);
                    // 这里需要将content全部release
                    contents.forEach(ReferenceCountUtil::release);
                    return;
                }

                Promise<Channel> promise = ctx.executor().newPromise();
                if (request.method().equals(HttpMethod.CONNECT)) {
                    promise.addListener(
                            (FutureListener<Channel>) future -> {
                                final Channel outboundChannel = future.getNow();
                                if (future.isSuccess()) {
                                    ChannelFuture responseFuture = ctx.channel().writeAndFlush(
                                            new DefaultHttpResponse(request.protocolVersion(), new HttpResponseStatus(200, "Connection Established")));
                                    responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                                        if (channelFuture.isSuccess()) {
                                            ctx.pipeline().remove(HttpRequestDecoder.class);
                                            ctx.pipeline().remove(HttpResponseEncoder.class);
                                            ctx.pipeline().remove(HttpServerExpectContinueHandler.class);
                                            ctx.pipeline().remove(HttpProxyConnectHandler.class);
                                            outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                            ctx.pipeline().addLast(new RelayHandler(outboundChannel));
//                                                    ctx.channel().config().setAutoRead(true);
                                        } else {
                                            log.info("reply tunnel established Failed: " + ctx.channel().remoteAddress() + " " + request.method() + " " + request.uri());
                                            SocksServerUtils.closeOnFlush(ctx.channel());
                                            SocksServerUtils.closeOnFlush(outboundChannel);
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
                            .handler(new com.arloor.forwardproxy.DirectClientHandler(promise));

                    b.connect(host,port).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.info("connect host {} port {}",host,port);
                        } else {
                            ctx.channel().writeAndFlush(
                                    new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                            );
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    });

                } else {
                    promise.addListener(
                            (FutureListener<Channel>) future -> {
                                final Channel outboundChannel = future.getNow();
                                if (future.isSuccess()) {
                                    ctx.pipeline().remove(HttpProxyConnectHandler.this);
                                    ctx.pipeline().remove(HttpResponseEncoder.class);
                                    outboundChannel.pipeline().addLast(new HttpRequestEncoder());
                                    outboundChannel.pipeline().addLast(new com.arloor.forwardproxy.RelayHandler(ctx.channel()));
                                    com.arloor.forwardproxy.RelayHandler clientEndtoRemoteHandler = new com.arloor.forwardproxy.RelayHandler(outboundChannel);
                                    ctx.pipeline().addLast(clientEndtoRemoteHandler);
                                    RemotePojo remotePojo = new RemotePojo();
                                    remotePojo.setU(request.uri());

                                    contents.forEach(o->{
                                        remotePojo.getC().add(o.content().array());
                                    });

                                    remotePojo.setM(request.method().name());
                                    remotePojo.setHv(request.protocolVersion().protocolName());
                                    HashMap<String, String> headerMap = new HashMap<>();
                                    List<Map.Entry<String, String>> entries = request.headers().entries();
                                    for (Map.Entry<String, String> entry : entries) {
                                        headerMap.put(entry.getKey(),entry.getValue());
                                    }
                                    remotePojo.setHe(headerMap);
                                    clientEndtoRemoteHandler.channelRead(ctx, remotePojo);
                                    ReferenceCountUtil.release(request);
                                    ReferenceCountUtil.release(contents);

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
                            .handler(new com.arloor.forwardproxy.DirectClientHandler(promise));
                    b.connect("42.192.169.194",10189).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.info("connect host {} port {}","localhost",9077);
                        } else {
                            ctx.channel().writeAndFlush(
                                    new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                            );
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    });
                }



            }
        }
    }

    private String getUserName(String basicAuth, Map<String, String> auths) {
        String userName = "";
        if (basicAuth != null && basicAuth.length() != 0) {
            String raw = auths.get(basicAuth);
            if (raw != null && raw.length() != 0) {
                userName = raw.split(":")[0];
            }
        }
        return userName;
    }

    private void setHostPort(ChannelHandlerContext ctx) {
        String hostAndPortStr = request.headers().get("Host");
        if (hostAndPortStr == null) {
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
        String[] hostPortArray = hostAndPortStr.split(":");
        host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        port = Integer.parseInt(portStr);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
