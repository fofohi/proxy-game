package com.arloor.forwardproxy.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class RemotePojo implements Serializable {

    private String uri;

    private List<String> content = new ArrayList<>();

    private String httpVersion;

    private String method;

    private Map<String,String> headers;


}
