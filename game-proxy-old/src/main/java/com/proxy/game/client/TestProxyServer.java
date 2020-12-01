package com.proxy.game.client;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyConnection;

import java.util.concurrent.TimeUnit;

/**
 * undertow
 */
public class TestProxyServer extends LoadBalancingProxyClient {

    TestProxyServer(){
        super();
    }

    @Override
    public ProxyTarget findTarget(HttpServerExchange exchange) {
        if(!exchange.getHostName().contains("granbluefantasy")){
            return null;
        }
        return super.findTarget(exchange);
    }

    @Override
    protected Host selectHost(HttpServerExchange exchange) {
        String[] x = exchange.getRequestPath().split("\\.");
        //todo md5
        if(x.length > 1 && x[0].contains("banner_event_start_1")){
            //get pic then save
            String[] xx = x[0].split("/");
            String picUrl = "http://localhost:9079/" + xx[xx.length - 1] + "." + x[1];
            Host host = super.selectHost(exchange);
            while (!host.getUri().getHost().contains("localhost")){
                host = super.selectHost(exchange);
            }
            exchange.setRequestURI(picUrl);
            return host;
        }else{
            Host host = super.selectHost(exchange);
            while (host.getUri().getHost().contains("localhost")){
                host = super.selectHost(exchange);
            }
            return host;
        }
    }

    @Override
    public void getConnection(ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {

        super.getConnection(target,exchange,callback,timeout,timeUnit);
    }
}
