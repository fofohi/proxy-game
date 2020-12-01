package com.proxy.game.netty.pra;

import com.proxy.game.netty.pojo.KryoMsgEncoder;
import com.proxy.game.netty.pojo.RemotePojo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class PraHttpInboundHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext browserToLocalServerChannel, Object msg){
        log.info("msg {}",msg);
        if(msg instanceof FullHttpRequest){
            FullHttpRequest fMsg = (FullHttpRequest) msg;
            if(fMsg.method().equals(HttpMethod.CONNECT)){
                SocksServerUtils.closeOnFlush(browserToLocalServerChannel.channel());
            }else{
                Promise<Channel> promise = browserToLocalServerChannel.executor().newPromise();
                promise.addListener(
                        (FutureListener<Channel>) future -> {
                            Channel localServerToRemoteChannel = future.getNow();
                            if (future.isSuccess()) {
                                RemotePojo remotePojo = new RemotePojo();


                                remotePojo.setUri(fMsg.uri());
                                remotePojo.setContent(fMsg.content().toString(CharsetUtil.UTF_8));
                                remotePojo.setMethod(fMsg.method().name());
                                remotePojo.setHttpVersion(fMsg.protocolVersion().protocolName());
                                HashMap<String, String> headerMap = new HashMap<>();
                                List<Map.Entry<String, String>> entries = fMsg.headers().entries();
                                for (Map.Entry<String, String> entry : entries) {
                                    headerMap.put(entry.getKey(),entry.getValue());
                                }
                                remotePojo.setHeaders(headerMap);
                                //往服务器写解析的http请求
                                localServerToRemoteChannel.pipeline().addLast("k1",new KryoMsgEncoder());
                                //localServerToRemoteChannel.pipeline().addLast(new ToClientDecoder());
                                localServerToRemoteChannel.pipeline().addLast(new PraByteHandler(browserToLocalServerChannel));
                                localServerToRemoteChannel.writeAndFlush(remotePojo);
                            } else {
                            /*
                            500
                            ctx.channel().writeAndFlush(
                                    new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
                            );*/
                                //SocksServerUtils.closeOnFlush(browserToLocalServerChannel.channel());
                            }//
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
                            }
                        })
                ;
                b.connect("localhost",9077).addListener((ChannelFutureListener) future -> {


                });
                /*b.connect("162.14.8.228", 19077).addListener((ChannelFutureListener) future -> {

                });*/

            }
        }

    }
}
