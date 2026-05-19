package com.alicp.jetcache.redis.lettuce;

import com.alicp.jetcache.CacheConfigException;
import com.alicp.jetcache.CacheException;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created on 2017/4/28.
 *
 * @author huangli
 */
public class LettuceConnectionManager {

    private static class LettuceObjects {
        private StatefulConnection connection;
        private StatefulRedisPubSubConnection pubSubConnection;
        private Object commands;
        private Object asyncCommands;
        private Object reactiveCommands;
    }

    private static final LettuceConnectionManager defaultManager = new LettuceConnectionManager();

    private final Map<AbstractRedisClient, LettuceObjects> map = new HashMap<>();

    private LettuceConnectionManager() {
    }

    public static LettuceConnectionManager defaultManager() {
        return defaultManager;
    }

    private LettuceObjects getLettuceObjectsFromMap(AbstractRedisClient redisClient) {
        LettuceObjects lo = map.get(redisClient);
        if (lo == null) {
            throw new CacheException("LettuceObjects is not initialized");
        }
        return lo;
    }

    /**
     * @deprecated use {@link #init(AbstractRedisClient, StatefulConnection, StatefulRedisPubSubConnection)} instead
     */
    @Deprecated
    public synchronized void init(AbstractRedisClient redisClient, StatefulConnection connection) {
        init(redisClient, connection, null);
    }

    /**
     * Initialize with existing connections. Only sets fields that are not already set.
     * @param redisClient the redis client
     * @param connection the normal connection, can be null
     * @param pubSubConnection the pub/sub connection, can be null
     */
    public synchronized void init(AbstractRedisClient redisClient, StatefulConnection connection, StatefulRedisPubSubConnection pubSubConnection) {
        LettuceObjects lo = map.computeIfAbsent(redisClient, key -> new LettuceObjects());
        if (connection != null && lo.connection == null) {
            lo.connection = connection;
        }
        if (pubSubConnection != null && lo.pubSubConnection == null) {
            lo.pubSubConnection = pubSubConnection;
        }
    }

    public synchronized StatefulConnection connection(AbstractRedisClient redisClient) {
        LettuceObjects lo = getLettuceObjectsFromMap(redisClient);
        if (lo.connection == null) {
            if (redisClient instanceof RedisClient) {
                lo.connection = ((RedisClient) redisClient).connect(new JetCacheCodec());
            } else if (redisClient instanceof RedisClusterClient) {
                lo.connection = ((RedisClusterClient) redisClient).connect(new JetCacheCodec());
            } else {
                throw new CacheConfigException("type " + redisClient.getClass() + " is not supported");
            }
        }
        return lo.connection;
    }

    public synchronized StatefulRedisPubSubConnection pubSubConnection(AbstractRedisClient redisClient) {
        LettuceObjects lo = getLettuceObjectsFromMap(redisClient);
        if (lo.pubSubConnection == null) {
            if (redisClient instanceof RedisClient) {
                lo.pubSubConnection = ((RedisClient) redisClient).connectPubSub(new JetCacheCodec());
            } else if (redisClient instanceof RedisClusterClient) {
                lo.pubSubConnection = ((RedisClusterClient) redisClient).connectPubSub(new JetCacheCodec());
            } else {
                throw new CacheConfigException("type " + redisClient.getClass() + " is not supported");
            }
        }
        return lo.pubSubConnection;
    }

    public synchronized Object commands(AbstractRedisClient redisClient) {
        connection(redisClient);
        LettuceObjects lo = getLettuceObjectsFromMap(redisClient);
        if (lo.commands == null) {
            if (lo.connection instanceof StatefulRedisConnection) {
                lo.commands = ((StatefulRedisConnection) lo.connection).sync();
            } else if (lo.connection instanceof StatefulRedisClusterConnection) {
                lo.commands = ((StatefulRedisClusterConnection) lo.connection).sync();
            } else {
                throw new CacheConfigException("type " + lo.connection.getClass() + " is not supported");
            }
        }
        return lo.commands;
    }

    public synchronized Object asyncCommands(AbstractRedisClient redisClient) {
        connection(redisClient);
        LettuceObjects lo = getLettuceObjectsFromMap(redisClient);
        if (lo.asyncCommands == null) {
            if (lo.connection instanceof StatefulRedisConnection) {
                lo.asyncCommands = ((StatefulRedisConnection) lo.connection).async();
            } else if (lo.connection instanceof StatefulRedisClusterConnection) {
                lo.asyncCommands = ((StatefulRedisClusterConnection) lo.connection).async();
            } else {
                throw new CacheConfigException("type " + lo.connection.getClass() + " is not supported");
            }
        }
        return lo.asyncCommands;
    }

    public synchronized Object reactiveCommands(AbstractRedisClient redisClient) {
        connection(redisClient);
        LettuceObjects lo = getLettuceObjectsFromMap(redisClient);
        if (lo.reactiveCommands == null) {
            if (lo.connection instanceof StatefulRedisConnection) {
                lo.reactiveCommands = ((StatefulRedisConnection) lo.connection).reactive();
            } else if (lo.connection instanceof StatefulRedisClusterConnection) {
                lo.reactiveCommands = ((StatefulRedisClusterConnection) lo.connection).reactive();
            } else {
                throw new CacheConfigException("type " + lo.connection.getClass() + " is not supported");
            }
        }
        return lo.reactiveCommands;
    }

    public synchronized void removeAndClose(AbstractRedisClient redisClient) {
        LettuceObjects lo = map.remove(redisClient);
        if (lo == null) {
            return;
        }
        /*
        if (lo.commands != null && lo.commands instanceof RedisClusterCommands) {
            ((RedisClusterCommands) lo.commands).close();
        }
        if (lo.asyncCommands != null && lo.asyncCommands instanceof RedisClusterAsyncCommands) {
            ((RedisClusterAsyncCommands) lo.asyncCommands).close();
        }
        if (lo.reactiveCommands != null && lo.reactiveCommands instanceof RedisClusterReactiveCommands) {
            ((RedisClusterReactiveCommands) lo.reactiveCommands).close();
        }
        */
        if (lo.connection != null) {
            lo.connection.close();
        }
        if (lo.pubSubConnection != null) {
            lo.pubSubConnection.close();
        }
        redisClient.shutdown();
    }
}
