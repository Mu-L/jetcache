package com.alicp.jetcache.autoconfigure;

import com.alicp.jetcache.redis.lettuce.LettuceConnectionManager;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

public class RedisLettuceAutoConfigurationTest {

    @Test
    public void autoInitRegistersPubSubConnectionWithoutBroadcastChannel() throws Exception {
        AutoConfigureBeans beans = new AutoConfigureBeans();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Collections.singletonMap(
                "jetcache.remote.default.type", "redis.lettuce")));
        environment.getPropertySources().addFirst(new MapPropertySource("uri", Collections.singletonMap(
                "jetcache.remote.default.uri", "redis://127.0.0.1:6379/")));

        TestRedisLettuceAutoInit autoInit = new TestRedisLettuceAutoInit();
        autoInit.autoConfigureBeans = beans;
        autoInit.environment = environment;

        try {
            autoInit.afterPropertiesSet();

            Assertions.assertSame(autoInit.pubSubConnection,
                    beans.getCustomContainer().get("remote.default.pubSubConnection"));
            LettuceFactory factory = new LettuceFactory(beans, "remote.default", StatefulRedisPubSubConnection.class);
            Assertions.assertSame(autoInit.pubSubConnection, factory.getObject());
        } finally {
            LettuceConnectionManager.defaultManager().removeAndClose(autoInit.client);
        }
    }

    private static class TestRedisLettuceAutoInit extends RedisLettuceAutoConfiguration.RedisLettuceAutoInit {
        private final RedisClient client = RedisClient.create();
        private final StatefulRedisConnection<byte[], byte[]> connection = proxy(StatefulRedisConnection.class);
        private final StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection =
                proxy(StatefulRedisPubSubConnection.class);

        @Override
        protected RedisClient createRedisClient() {
            return client;
        }

        @Override
        protected StatefulConnection<byte[], byte[]> connect(RedisClient client, List<RedisURI> uriList, ReadFrom readFrom) {
            return connection;
        }

        @Override
        protected StatefulRedisPubSubConnection<byte[], byte[]> connectPubSub(RedisClient client, RedisURI redisURI) {
            return pubSubConnection;
        }
    }

    private static <T> T proxy(Class<T> clazz) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("sync".equals(method.getName())) {
                return proxy(RedisCommands.class);
            } else if ("async".equals(method.getName())) {
                return proxy(RedisAsyncCommands.class);
            } else if ("reactive".equals(method.getName())) {
                return proxy(RedisReactiveCommands.class);
            }
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
