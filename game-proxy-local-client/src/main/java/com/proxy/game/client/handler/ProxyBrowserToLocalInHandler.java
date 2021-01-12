package com.proxy.game.client.handler;


import com.proxy.game.pojo.RemotePojo;
import com.proxy.game.pojo.context.HandlerContext;
import com.proxy.game.pojo.util.MsgEncoder;
import com.proxy.game.pojo.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 浏览器到本地代理的入口
 */
@Slf4j
public class ProxyBrowserToLocalInHandler extends ChannelInboundHandlerAdapter {

    private Channel localChannel;

    private static NioEventLoopGroup thisGroup = new NioEventLoopGroup(2);

    @Override
    public void channelRead(final ChannelHandlerContext browserAndServerChannel, Object msg){
        if(msg instanceof FullHttpRequest){
            localChannel = browserAndServerChannel.channel();
            log.info("full http request {}",msg);
            final FullHttpRequest fullMsg = (FullHttpRequest) msg;
            if(fullMsg.method().equals(HttpMethod.CONNECT)){
                SocksServerUtils.closeOnFlush(browserAndServerChannel.channel());
            }else{
                final Promise<Channel> browserAndServerPromise = browserAndServerChannel.executor().newPromise();
                browserAndServerPromise.addListener(new FutureListener<Channel>() {
                    @Override
                    public void operationComplete(Future<Channel> future) {
                        RemotePojo remotePojo = new RemotePojo();
                        remotePojo.setUri(fullMsg.uri());
                        ByteBuf bf = Unpooled.buffer();
                        bf.writeBytes(fullMsg.content());
                        remotePojo.getContent().add(bf.array());
                        bf.release();
                        remotePojo.setMethod(fullMsg.method().name());
                        remotePojo.setHttpVersion(fullMsg.protocolVersion().protocolName());
                        HashMap<String, String> headerMap = new HashMap<>();
                        List<Map.Entry<String, String>> entries = fullMsg.headers().entries();
                        for (Map.Entry<String, String> entry : entries) {
                            headerMap.put(entry.getKey(),entry.getValue());
                        }
                        remotePojo.setHeaders(headerMap);

                        future.getNow().writeAndFlush(remotePojo);
                        log.info("success {}",Thread.currentThread().getName());
                    }
                });

                log.info("success {}",Thread.currentThread().getName());
                //本地server连接远程server
                Bootstrap b = new Bootstrap();
                b.group(thisGroup)
                        .channel(localChannel.getClass())
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel ch) {
                                ch.pipeline().addLast(new PraByteHandler(browserAndServerChannel));
                                ch.pipeline().addLast(HandlerContext.PROXY_MSG_ENCODER,new MsgEncoder());
                            }
                        });
                //todo config remote server ip
                //localServerAndRemoteServerChannel
                b.connect("localhost",9077).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        log.info("connect localhost 9077 success {}",Thread.currentThread().getName());
                        browserAndServerPromise.setSuccess(future.channel());
                    }
                });
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception {}",cause.getMessage());
    }
}
