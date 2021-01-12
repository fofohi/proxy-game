package com.proxy.game.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.proxy.game.pojo.RemotePojo;
import com.proxy.game.pojo.util.MsgDecoder;
import com.proxy.game.pojo.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ProxyRemoteToLocalHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RemotePojo remotePojo = (RemotePojo) msg;
        StringBuilder contents = new StringBuilder();
        for (byte[] bytes : remotePojo.getContent()) {
            contents.append(new String(bytes));
        }
        log.info("msg content {}", contents.toString());
        reconnectAndFlush(remotePojo, ctx);
    }

    private void reconnectAndFlush(RemotePojo pojo, ChannelHandlerContext ctx) {
        final Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                (FutureListener<Channel>) future -> {
                    Channel remoteServerToNg = future.getNow();
                    if (future.isSuccess()) {
                        ByteBuf bf = Unpooled.buffer();
                        for (byte[] s : pojo.getContent()) {
                            bf.writeBytes(s);
                        }
                        DefaultFullHttpRequest full = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                                new HttpMethod(pojo.getMethod()),
                                pojo.getUri().replace("http://" + pojo.getHeaders().get("Host"), ""), bf);
                        for (Map.Entry<String, String> stringStringEntry : pojo.getHeaders().entrySet()) {
                            full.headers().add(stringStringEntry.getKey(), stringStringEntry.getValue());
                        }
                        //往真正的服务器写
                        remoteServerToNg.writeAndFlush(full);
                    }
                });
        final Bootstrap b2 = new Bootstrap();
        b2.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpRequestEncoder());
                        ch.pipeline().addLast(new HttpResponseDecoder());
                        ch.pipeline().addLast(new HttpObjectAggregator(102400000));
                        ch.pipeline().addLast(new ProxyRemoteTargetHandler(ctx));
                    }
                })
        ;
        String hostAndPort = pojo.getHeaders().get("Host");
        String[] hostAndPortString = hostAndPort.split(":");
        b2.connect(hostAndPortString[0], hostAndPortString.length > 1 ? Integer.parseInt(hostAndPortString[1]) : 80).addListener((ChannelFutureListener) future -> {
            promise.setSuccess(future.channel());
        });
    }
}
