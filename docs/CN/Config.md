# 配置说明
yml配置文件案例（如果没使用springboot，直接配置GlobalCacheConfig是类似的，参考快速入门教程）：
```
jetcache:
  statIntervalMinutes: 15
  areaInCacheName: false
  hidePackages: com.alibaba
  local:
    default:
      type: caffeine
      limit: 100
      keyConvertor: fastjson2 #其他可选：fastjson(等同fastjson2)/jackson/jackson3
      expireAfterWriteInMillis: 100000
    otherArea:
      type: linkedhashmap
      limit: 100
      keyConvertor: none
      expireAfterWriteInMillis: 100000
  remote:
    default:
      type: redis
      keyConvertor: fastjson2 #其他可选：fastjson(等同fastjson2)/jackson/jackson3
      broadcastChannel: projectA
      valueEncoder: java #其他可选：kryo/kryo5
      valueDecoder: java #其他可选：kryo/kryo5
      poolConfig:
        minIdle: 5
        maxIdle: 20
        maxTotal: 50
      host: ${redis.host}
      port: ${redis.port}
    otherArea:
      type: redis
      keyConvertor: fastjson2 #其他可选：fastjson(等同fastjson2)/jackson/jackson3
      broadcastChannel: projectA
      valueEncoder: java #其他可选：kryo/kryo5
      valueDecoder: java #其他可选：kryo/kryo5
      poolConfig:
        minIdle: 5
        maxIdle: 20
        maxTotal: 50
      host: ${redis.host}
      port: ${redis.port}
```


配置通用说明如下

| 属性 | 默认值                         | 说明                                                                                                                                                                                                    |
| --- |-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| jetcache.statIntervalMinutes | 0                           | 统计间隔，0表示不统计                                                                                                                                                                                           |
| jetcache.areaInCacheName | true(2.6-) false(2.7+)      | jetcache-anno把cacheName作为远程缓存key前缀，2.4.3以前的版本总是把areaName加在cacheName中，因此areaName也出现在key前缀中。2.4.4以后可以配置，为了保持远程key兼容默认值为true，但是新项目的话false更合理些，2.7默认值已改为false。                                            |
| jetcache.hiddenPackages | 无                           | @Cached和@CreateCache自动生成name的时候，为了不让name太长，hiddenPackages指定的包名前缀被截掉                                                                                                                                   |
| jetcache.useDefaultLocalExpireInMultiLevelCache | false | 如果设置为true，当cacheType为BOTH且未显式设置localExpire时（包括`@Cached`、`@CreateCache`注解和`QuickConfig` API），本地缓存的过期时间取本地缓存builder上配置的`expireAfterWriteInMillis`与`expire`中的较小值。 |
| jetcache.[local/remote].${area}.type | 无                           | 缓存类型。tair、redis为当前支持的远程缓存；linkedhashmap、caffeine为当前支持的本地缓存类型                                                                                                                                          |
| jetcache.[local/remote].${area}.keyConvertor | fastjson2 | key转换器的全局配置。2.8+支持的keyConvertor：```fastjson2```/```jackson```/```jackson3```（```fastjson```也可用，内部使用fastjson2实现）；仅当使用@CreateCache且缓存类型为LOCAL时可以指定为```none```，此时通过equals方法来识别key。方法缓存必须指定keyConvertor |
| jetcache.[local/remote].${area}.valueEncoder | java                        | 序列化器的全局配置。仅remote类型的缓存需要指定，2.8+可选```java```/```kryo```/```kryo5```（```kryo```和```kryo5```均使用kryo5实现） |
| jetcache.[local/remote].${area}.valueDecoder | java                        | 反序列化器的全局配置。仅remote类型的缓存需要指定，2.8+可选```java```/```kryo```/```kryo5```（```kryo```和```kryo5```均使用kryo5实现） |
| jetcache.[local/remote].${area}.limit | 100                         | 每个缓存实例的最大元素的全局配置，仅local类型的缓存需要指定。注意是每个缓存实例的限制，而不是全部，比如这里指定100，然后用@CreateCache创建了两个缓存实例（并且注解上没有设置localLimit属性），那么每个缓存实例的限制都是100                                                                        |
| jetcache.[local/remote].${area}.expireAfterWriteInMillis | 无穷大                         | 以毫秒为单位指定超时时间的全局配置(以前为defaultExpireInMillis)                                                                                                                                                           |
| jetcache.remote.${area}.broadcastChannel | 无                           | jetcahe2.7的两级缓存支持更新以后失效其他JVM中的local cache，但多个服务共用redis同一个channel可能会造成广播风暴，需要在这里指定channel，你可以决定多个不同的服务是否共用同一个channel。如果没有指定则不开启。                                                                       |
| jetcache.local.${area}.expireAfterAccessInMillis | 0                           | 需要jetcache2.2以上，以毫秒为单位，指定多长时间没有访问，就让缓存失效，当前只有本地缓存支持。0表示不使用这个功能。                                                                                                                                       |
| jetcache.decodeFilterEnabled | true | 反序列化过滤器总开关，默认开启。关闭后恢复旧行为（不推荐关闭） |
| jetcache.decodeFilterPatterns | 无 | 用户自定义的允许列表包名前缀或完整类名列表，追加到默认允许列表之后 |

上表中${area}对应@Cached和@CreateCache的area属性。注意如果注解上没有指定area，默认值是"default"。

## 反序列化过滤器配置

JetCache 2.8.x 默认开启反序列化过滤器，只允许以下包下的类被反序列化：`java.lang`（仅直接类，不含子包如reflect/invoke）、`java.util.`、`java.time.`、`java.math.`、`java.net`（仅直接类，不含子包）、`com.alicp.jetcache.`。如果缓存值包含自定义类，需要配置允许列表：

```yaml
jetcache:
  decodeFilterEnabled: true  # 默认 true，可设 false 关闭
  decodeFilterPatterns:
    - com.example.          # 前缀匹配：com.example 包下所有类
    - org.myapp.dto.UserDTO # 精确匹配：仅允许这个类
```

也可以通过编程式 API 配置（非 Spring Boot 场景）：

```java
DecodeFilter filter = DecodeFilter.getDefault();
filter.setEnabled(true);
filter.addAllowPatterns("com.example.");
```

`DecodeFilter` 是非单例设计，有一个通过 `getDefault()` 获取的默认实例。如果需要隔离（如同 JVM 多上下文），可以创建独立实例：

```java
DecodeFilter myFilter = new DecodeFilter(myAllowPatterns, myDenyPatterns);
```

默认实例的允许列表和拒绝列表可以通过编程式 API 管理：

```java
DecodeFilter filter = DecodeFilter.getDefault();
filter.addAllowPatterns("com.example.", "org.myapp.");
filter.removeAllowPatterns("org.myapp.");
filter.clearAllowPatterns();
filter.addDenyPatterns("com.dangerous.");
filter.removeDenyPatterns("com.dangerous.");
filter.clearDenyPatterns();
```

如果反序列化时被拒绝，会输出ERROR日志（包含被拒绝的类名和配置示例），并抛出异常。Kryo和JSON路径抛出`DecodeFilterException`；Java序列化路径抛出`InvalidClassException`（JDK内部行为）。

**注意**：
- JDK动态代理类（如`com.sun.proxy.$Proxy*`、`jdk.proxy*`）不在默认允许列表中。如果缓存了代理对象（如Spring AOP代理），需要将对应包名添加到允许列表。
- 默认允许列表不包含`java.io`、`java.rmi`、`java.beans`、`java.lang.reflect`、`javax.naming`等包。如果需要这些包中的类，可通过`decodeFilterPatterns`或`addAllowPatterns`添加。
- 默认黑名单包含部分高危包和类（如`java.rmi`、`javax.naming`、`com.sun.rowset`、`java.lang.Runtime`等），通过编程式 API 可以修改黑名单。

关于缓存的超时时间，有多个地方指定，澄清说明一下：
1. put等方法上指定了超时时间，则以此时间为准
1. put等方法上未指定超时时间，使用Cache实例的默认超时时间
1. Cache实例的默认超时时间，通过在@CreateCache和@Cached上的expire属性指定，如果没有指定，使用yml中定义的全局配置，例如@Cached(cacheType=local)使用jetcache.local.default.expireAfterWriteInMillis，如果仍未指定则是无穷大
