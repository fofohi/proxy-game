package com.arloor.forwardproxy.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class RemotePojo implements Serializable {

    /*uri*/
    private String u;

    /*content*/
    private List<String> c = new ArrayList<>();

    /*http version*/
    private String hv;

    /*method*/
    private String m;

    /*header*/
    private Map<String,String> he;


}
