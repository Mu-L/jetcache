package com.alicp.jetcache.autoconfigure;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LettuceFactoryTest {

    @Test
    public void pubSubConnectionUsesPubSubContainerEntry() throws Exception {
        AutoConfigureBeans beans = new AutoConfigureBeans();
        Object connection = new Object();
        Object pubSubConnection = new Object();
        beans.getCustomContainer().put("remote.default.connection", connection);
        beans.getCustomContainer().put("remote.default.pubSubConnection", pubSubConnection);

        LettuceFactory factory = new LettuceFactory(beans, "remote.default", StatefulRedisPubSubConnection.class);

        Assertions.assertSame(pubSubConnection, factory.getObject());
    }

    @Test
    public void statefulConnectionUsesConnectionContainerEntry() throws Exception {
        AutoConfigureBeans beans = new AutoConfigureBeans();
        Object connection = new Object();
        Object pubSubConnection = new Object();
        beans.getCustomContainer().put("remote.default.connection", connection);
        beans.getCustomContainer().put("remote.default.pubSubConnection", pubSubConnection);

        LettuceFactory factory = new LettuceFactory(beans, "remote.default", StatefulRedisConnection.class);

        Assertions.assertSame(connection, factory.getObject());
    }
}
