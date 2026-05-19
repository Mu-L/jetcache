package com.alicp.jetcache.redis.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class LettuceConnectionManagerTest {

    @Test
    public void initAddsPubSubConnectionToExistingClientEntry() {
        RedisClient client = RedisClient.create("redis://127.0.0.1");
        StatefulConnection<byte[], byte[]> connection = proxy(StatefulConnection.class);
        StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection = proxy(StatefulRedisPubSubConnection.class);
        LettuceConnectionManager manager = LettuceConnectionManager.defaultManager();
        try {
            manager.init(client, connection);
            manager.init(client, connection, pubSubConnection);

            Assertions.assertSame(pubSubConnection, manager.pubSubConnection(client));
        } finally {
            manager.removeAndClose(client);
        }
    }

    private <T> T proxy(Class<T> clazz) {
        InvocationHandler handler = (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();
            if (returnType == Boolean.TYPE) {
                return false;
            } else if (returnType == Byte.TYPE) {
                return (byte) 0;
            } else if (returnType == Short.TYPE) {
                return (short) 0;
            } else if (returnType == Integer.TYPE) {
                return 0;
            } else if (returnType == Long.TYPE) {
                return 0L;
            } else if (returnType == Float.TYPE) {
                return 0F;
            } else if (returnType == Double.TYPE) {
                return 0D;
            } else if (returnType == Character.TYPE) {
                return (char) 0;
            }
            return null;
        };
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }
}
