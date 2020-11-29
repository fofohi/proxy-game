package com.proxy.game.netty.pojo;

public class KryoSerializerFactory {

    public static KryoSerializer getSerializer(Class<?> cls) {
        return new KryoUtil(cls);
    }
}
