package com.proxy.game.netty.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class RemotePojo implements Serializable {

    private String uri;

    private List<byte[]> content = new ArrayList<>();

    private String httpVersion;

    private String method;

    private Map<String,String> headers;


}
