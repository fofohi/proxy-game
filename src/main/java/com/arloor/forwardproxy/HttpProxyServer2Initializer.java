package com.arloor.forwardproxy;

import com.arloor.forwardproxy.monitor.GlobalTrafficMonitor;
import com.arloor.forwardproxy.vo.Config;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class HttpProxyServer2Initializer extends ChannelInitializer<SocketChannel> {

    private final Config.Http http;

    public HttpProxyServer2Initializer(Config.Http http) throws IOException, GeneralSecurityException {
        this.http = http;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new KryoMsgDecoder());
        p.addLast(new HttpProxyConnect2Handler());
    }
}
