package com.proxy.game.client.handler;


import com.proxy.game.pojo.RemotePojo;
import com.proxy.game.pojo.context.HandlerContext;
import com.proxy.game.pojo.util.MsgEncoder;
import com.proxy.game.pojo.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.buffer.UnpooledHeapByteBuf;
import io.netty.channel.*;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.undertow.connector.PooledByteBuffer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 浏览器到本地代理的入口
 */
@Slf4j
public class ProxyBrowserToLocalInHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext browserAndServerChannel, Object msg){

        if(msg instanceof FullHttpRequest){
            Channel localChannel = browserAndServerChannel.channel();
            log.info("full http request {}",msg);
            final FullHttpRequest fullMsg = (FullHttpRequest) msg;
            if(fullMsg.method().equals(HttpMethod.CONNECT)){
                browserAndServerChannel.fireChannelRead(msg);
            }else{
                //todo config remote server ip
                //localServerAndRemoteServerChannel
                String hostAndPort = fullMsg.headers().get("Host");
                String[] hostAndPortString = hostAndPort.split(":");
                String host = (hostAndPortString[0].contains("granblue") || hostAndPortString[0].contains("gbf.game.mbga.jp")) ? "localhost" : hostAndPortString[0];
                int port = (hostAndPortString[0].contains("granblue") || hostAndPortString[0].contains("gbf.game.mbga.jp")) ? 9077 : hostAndPortString.length > 1 ? Integer.parseInt(hostAndPortString[1]) : 80;

                final Promise<Channel> browserAndServerPromise = browserAndServerChannel.executor().newPromise();
                browserAndServerPromise.addListener((FutureListener<Channel>) future -> {
                    if((hostAndPortString[0].contains("granblue") || hostAndPortString[0].contains("gbf.game.mbga.jp"))){
                        RemotePojo remotePojo = new RemotePojo();
                        remotePojo.setUri(fullMsg.uri());
                        if(fullMsg.content().isDirect()){
                            ByteBuf bf = Unpooled.buffer();
                            bf.writeBytes(fullMsg.content());
                            for (byte b : bf.array()) {
                                if(b != 0){
                                    remotePojo.getContent().add(b);
                                }
                            }
                            bf.release();
                        }else{
                            for (byte b : fullMsg.content().array()) {
                                if(b != 0){
                                    remotePojo.getContent().add(b);
                                }
                            }
                        }

                        remotePojo.setMethod(fullMsg.method().name());
                        remotePojo.setHttpVersion(fullMsg.protocolVersion().protocolName());
                        HashMap<String, String> headerMap = new HashMap<>();
                        List<Map.Entry<String, String>> entries = fullMsg.headers().entries();
                        for (Map.Entry<String, String> entry : entries) {
                            headerMap.put(entry.getKey(),entry.getValue());
                        }
                        remotePojo.setHeaders(headerMap);

                        future.getNow().writeAndFlush(remotePojo);
                    }else{
                        future.getNow().writeAndFlush(fullMsg);
                    }
                });

                //本地server连接远程server
                Bootstrap b = new Bootstrap();
                b.group(browserAndServerChannel.channel().eventLoop())
                        .channel(localChannel.getClass())
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel ch) {
                                if((hostAndPortString[0].contains("granblue") || hostAndPortString[0].contains("gbf.game.mbga.jp"))){
                                    ch.pipeline().addLast(HandlerContext.PROXY_MSG_ENCODER,new MsgEncoder());
                                }else{
                                    ch.pipeline().addLast(new HttpRequestEncoder());
                                }
                                ch.pipeline().addLast(new ProxyToClientByteHandler(browserAndServerChannel));
                            }
                        });

                b.connect(host,port).addListener((ChannelFutureListener) future -> {
                    log.info("connect " + host + " :" + port + " success {}",Thread.currentThread().getName());
                    browserAndServerPromise.setSuccess(future.channel());
                });
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception {}",cause.getMessage());
    }
}
