package com.proxy.game.client;

import io.undertow.Undertow;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.resource.PathResourceManager;

import java.net.URI;
import java.nio.file.Paths;

import static io.undertow.Handlers.resource;

public class UndertowServer {

    public static void main(final String[] args) {
        try {
            //本地undertow->验证服务器->入口
            LoadBalancingProxyClient loadBalancer = new TestProxyServer()
                    .addHost(new URI("http://localhost:9079"))
                    .setConnectionsPerThread(20);

            Undertow server3 = Undertow.builder()
                    .addHttpListener(9011, "localhost")
                    .setHandler(resource(new PathResourceManager(Paths.get("F:\\data"), 100))
                            .setDirectoryListingEnabled(false))
                    .build();
            server3.start();


            Undertow reverseProxy = Undertow.builder()
                    .addHttpListener(9012, "localhost")
                    .setIoThreads(4)
                    .setHandler(new ProxyHandler(loadBalancer, 30000, ResponseCodeHandler.HANDLE_404))
                    .build();
            reverseProxy.start();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
