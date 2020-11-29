package com.proxy.game.netty.pra;

import com.esotericsoftware.kryo.KryoException;
import com.proxy.game.netty.pojo.KryoMsgEncoder;
import com.proxy.game.netty.pojo.KryoUtil;
import com.proxy.game.netty.pojo.RemotePojo;
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

    @Override
    public void channelRead(ChannelHandlerContext browserToLocalServerChannel, Object msg){

        if(msg instanceof FullHttpRequest){
            Promise<Channel> promise = browserToLocalServerChannel.executor().newPromise();
            promise.addListener(
                    (FutureListener<Channel>) future -> {
                        //当前客户端对iplc的链接
                        Channel localServerToRemoteChannel = future.getNow();
                        if (future.isSuccess()) {
                            RemotePojo remotePojo = new RemotePojo();
                            FullHttpRequest fMsg = (FullHttpRequest) msg;
                            if(fMsg.uri().contains("443")) return;

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
                            localServerToRemoteChannel.pipeline().addLast(new KryoMsgEncoder());
                            localServerToRemoteChannel.writeAndFlush(remotePojo);
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
            b.connect("localhost",9077).addListener((ChannelFutureListener) future -> {
            });
        }

    }
}
