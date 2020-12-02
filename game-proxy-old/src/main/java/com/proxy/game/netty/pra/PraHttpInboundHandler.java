package com.proxy.game.netty.pra;

import com.proxy.game.netty.pojo.KryoMsgEncoder;
import com.proxy.game.netty.pojo.RemotePojo;
import com.proxy.game.ssl.TestSsl2Handler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class PraHttpInboundHandler extends ChannelInboundHandlerAdapter {


    private ArrayList<HttpContent> contents = new ArrayList<>();

    private HttpRequest request;

    private boolean sslOk = false;

    private Channel remoteChannel;

    private Channel clientChannel;


    @Override
    public void channelRead(ChannelHandlerContext browserAndServer, Object msg){
        clientChannel = browserAndServer.channel();

        if (sslOk) {
            remoteChannel.writeAndFlush(msg);
            return;
        }

        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            return;
        }else {
            contents.add((HttpContent) msg);
            if (request.method().equals(HttpMethod.CONNECT)) {
                clientChannel.config().setAutoRead(false);
                ChannelFuture sslFuture = clientChannel.writeAndFlush(Unpooled.wrappedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes()));
                sslFuture.addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess() && future.isDone()){
                        clientChannel.pipeline().remove(HttpRequestDecoder.class);
                        sslOk = true;
                    }
                });

                String hostAndPortString = request.headers().get("Host");
                String[] hostAndPort = hostAndPortString.split(":");
                Bootstrap b = new Bootstrap();
                b.group(clientChannel.eventLoop())
                        .channel(clientChannel.getClass())
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new TestSsl2Handler(clientChannel));
                            }
                        });
                ChannelFuture f = b.connect(hostAndPort[0], hostAndPort.length > 1 ? Integer.parseInt(hostAndPort[1]) : 80);
                remoteChannel = f.channel();

                f.addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        clientChannel.config().setAutoRead(true);
                        remoteChannel.writeAndFlush(request);
                        contents.forEach(o -> remoteChannel.writeAndFlush(o));
                    } else {
                        SocksServerUtils.closeOnFlush(clientChannel);
                        SocksServerUtils.closeOnFlush(remoteChannel);
                    }
                });
            }else{
                Promise<Channel> promise = browserAndServer.executor().newPromise();
                promise.addListener(
                        (FutureListener<Channel>) future -> {
                            Channel localServerToRemoteChannel = future.getNow();
                            if (future.isSuccess()) {
                                RemotePojo remotePojo = new RemotePojo();
                                remotePojo.setUri(request.uri());
                                List<byte[]> cs = new ArrayList<>() ;
                                for (HttpContent content : contents) {
                                    if(content.content().hasArray()){
                                        cs.add(content.content().array());
                                    }
                                }
                                remotePojo.setContent(cs);
                                remotePojo.setMethod(request.method().name());
                                remotePojo.setHttpVersion(request.protocolVersion().protocolName());
                                HashMap<String, String> headerMap = new HashMap<>();
                                List<Map.Entry<String, String>> entries = request.headers().entries();
                                for (Map.Entry<String, String> entry : entries) {
                                    headerMap.put(entry.getKey(),entry.getValue());
                                }
                                remotePojo.setHeaders(headerMap);
                                //往服务器写解析的http请求
                                localServerToRemoteChannel.pipeline().addLast("k1",new KryoMsgEncoder());
                                localServerToRemoteChannel.pipeline().addLast(new PraByteHandler(browserAndServer));
                                localServerToRemoteChannel.writeAndFlush(remotePojo);
                            } else {
                                SocksServerUtils.closeOnFlush(browserAndServer.channel());
                            }
                        });
                final Bootstrap b = new Bootstrap();
                b.group(browserAndServer.channel().eventLoop())
                        .channel(browserAndServer.channel().getClass())
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new PraHttpProxyHandler(promise));
                            }
                        })
                ;
                b.connect("162.14.8.228",19077).addListener((ChannelFutureListener) future -> {


                });
            }
        }
    }
}
