package com.proxy.game.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class RemoteToLocalPojo implements Serializable {

    private byte[] content;

    private Map<String,String> headers;

}
