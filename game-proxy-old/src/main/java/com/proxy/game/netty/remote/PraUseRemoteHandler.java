package com.proxy.game.netty.remote;

import com.alibaba.fastjson.JSON;
import com.proxy.game.netty.pojo.RemotePojo;
import com.proxy.game.netty.pra.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PraUseRemoteHandler extends ChannelInboundHandlerAdapter {

    private RemotePojo tmpPojo = null;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RemotePojo pojo = JSON.parseObject((String) msg, RemotePojo.class);
        tmpPojo = pojo;
        //RemotePojo pojo = (RemotePojo) msg;
        log.info("uri {} contents {}",pojo.getUri(),pojo.getContent().size());
        reconnectAndFlush(pojo,ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        reconnectAndFlush(tmpPojo,ctx);
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    private void reconnectAndFlush(RemotePojo pojo,ChannelHandlerContext ctx){
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
                        ch.pipeline().addLast(new PraTargetHandler(ctx));
                    }
                })
        ;

        String hostAndPort = pojo.getHeaders().get("Host");

        String[] hostAndPortString = hostAndPort.split(":");


        b2.connect(hostAndPortString[0],hostAndPortString.length > 1 ? Integer.parseInt(hostAndPortString[1]) : 80).addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()){
                ByteBuf bf = Unpooled.buffer();
                for (byte[] s : pojo.getContent()) {
                    bf.writeBytes(s);
                }
                DefaultFullHttpRequest full = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                        new HttpMethod(pojo.getMethod()),
                        pojo.getUri().replace("http://" + pojo.getHeaders().get("Host"),""),bf);
                for (Map.Entry<String, String> stringStringEntry : pojo.getHeaders().entrySet()) {
                    full.headers().add(stringStringEntry.getKey(),stringStringEntry.getValue());
                }
                //往真正的服务器写
                future.channel().writeAndFlush(full);
            }
        });
    }
}
