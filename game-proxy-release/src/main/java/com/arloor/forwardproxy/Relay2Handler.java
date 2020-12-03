package com.arloor.forwardproxy;

import com.alibaba.fastjson.JSON;
import com.arloor.forwardproxy.util.SocksServerUtils;
import com.arloor.forwardproxy.vo.RemotePojo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

public final class Relay2Handler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(Relay2Handler.class);

    private final Channel relayChannel;

    public Relay2Handler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        relayChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {//删除代理特有的请求头
            HttpRequest request = (HttpRequest) msg;
            request.headers().remove("Proxy-Authorization");
            String proxyConnection = request.headers().get("Proxy-Connection");
            if (Objects.nonNull(proxyConnection)) {
                request.headers().set("Connection", proxyConnection);
                request.headers().remove("Proxy-Connection");
            }

            String hostAndPortStr = request.headers().get("Host");
            String[] hostPortArray = hostAndPortStr.split(":");
            String host = hostPortArray[0];
            String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
            int port = Integer.parseInt(portStr);

            try {
                String url = request.uri();
                int index = url.indexOf(host) + host.length();
                url = url.substring(index);
                if (url.startsWith(":")) {
                    url = url.substring(1 + String.valueOf(port).length());
                }
                request.setUri(url);
            } catch (Exception e) {
                log.error("无法获取url：" + request.uri() + " " + host);
            }
        }

        if (relayChannel.isActive()) {
            if(msg instanceof ByteBuf){
                ctx.pipeline().addLast(new HttpObjectAggregator(10240000));
                ctx.pipeline().addLast(new HttpResponseEncoder());
            }
            relayChannel.writeAndFlush(msg).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("FAILED "+ctx.channel().remoteAddress()+"  >>>>>  "+relayChannel.remoteAddress()+" "+msg.getClass().getSimpleName());
                    log.error("relay error!", future.cause());
                } else {
                    log.info("SUCCESS "+ctx.channel().remoteAddress()+"  >>>>>>>  "+relayChannel.remoteAddress()+" "+msg.getClass().getSimpleName());
                }
            });
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (relayChannel.isActive()) {
            SocksServerUtils.closeOnFlush(relayChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION]["+clientHostname+"] "+ cause.getMessage());
        ctx.close();
    }
}
