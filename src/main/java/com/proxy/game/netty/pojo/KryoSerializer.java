package com.proxy.game.netty.pojo;

import com.esotericsoftware.kryo.Kryo;

import java.io.Serializable;

public interface KryoSerializer extends Serializable {

    byte[] serialize(Object obj);

    /**
     * 反序列化
     * @param bytes 字节数组
     * @return
     */
    <T> T deserialize(byte[] bytes);

    Kryo get();
}
