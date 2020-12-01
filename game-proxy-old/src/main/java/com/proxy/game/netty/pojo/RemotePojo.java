package com.proxy.game.netty.pojo;

import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class RemotePojo implements Serializable {

    private String uri;

    private String content;

    private String httpVersion;

    private String method;

    private Map<String,String> headers;


}
