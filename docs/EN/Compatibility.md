# spring compatibility
jetcache tested with below spring/spring-boot versions

| jetcache | spring                      | spring boot                 | comments                                                                                                                 |
|----------|-----------------------------|-----------------------------|--------------------------------------------------------------------------------------------------------------------------|
| 2.5      | 4.0.8.RELEASE~5.1.1.RELEASE | 1.1.9.RELEASE~2.0.5.RELEASE ||
| 2.6      | 5.0.4.RELEASE~5.2.4.RELEASE | 2.0.0.RELEASE~2.2.5.RELEASE | jetcache-redis depends on jedis3.1.0, spring-data(jedis, boot version<=2.1.X) depends on jedis2.9.3, can't used together |
| 2.7      | 5.2.4.RELEASE~5.3.23        | 2.2.5.RELEASE~2.7.5         | jetcache-redis depends on jedis4, spring-data(jedis) depends on jedis3, can't used together                              |
| 2.7.4      | 5.2.4.RELEASE~6.2.18        | 2.2.5.RELEASE~3.5.14        | can also support Spring 7/Spring Boot 4, but the BOM defines Spring 6/Spring Boot 3 by default |
| 2.8        | 6.x~7.0.7                   | 3.x~4.0.6                   | requires Java 17+; BOM defaults to Spring Framework 7.0.7 / Spring Boot 4.0.6 / Spring Data Redis 4.0.5 / SLF4J 2.x |

# compatible change notes
## 2.8.0
* Java 17 is now the minimum required version
* Removed fastjson1 support, `fastjson` key convertor now uses fastjson2 internally. If you need fastjson1, add the dependency yourself and implement a custom KeyConvertor
* Removed Spring XML namespace support (`<jetcache:xxx>` tags in XML configuration are no longer available)
* kryo4 is no longer supported, `com.esotericsoftware:kryo` is upgraded to 5.x. The `KRYO` constant in `SerialPolicy` now uses kryo5 implementation internally. kryo4 serialized data is not compatible with kryo5, wait for old cache entries to expire or clear cache before upgrading
* Removed `IDENTITY_NUMBER_KRYO4` constant
* `areaInCacheName` default value is now `false` (was `true` in versions prior to 2.8.0). 

## 2.7.4
* use spring-boot 3.1.3, spring-framework 6.0.11, slf4j-api 2.x as default
* remove javax.annotation:javax.annotation-api, if you use @PostConstruct, you may need to add this dependency by yourself

## 2.7.2
* update encoder/decoder of redisson, not compatible with 2.7.1

## 2.7.0
* jetcache-redis depends on jedis4，springdata(jedis) depends on jedis3, can't use together
* encoder/decoder now support kryo5. In yml "kryo" maps to `com.esotericsoftware:kryo` (5.x), "kryo5" maps to `com.esotericsoftware.kryo:kryo5`. Both use kryo5 serialization format and are wire-compatible, but have different maven coordinates and Java package names, so they can coexist
* use lettuce to connect redis cluster need specify "mode=cluster" in yml
* default key convertor change to "fastjson2", fastjson2 and fastjson can be used together, fastjson(not fastjson2)/kryo/kryo5/mvel is now optional in maven
* if not use spring boot, add ```@Import(JetCacheBaseBeans.class)```, and remove old configProvider bean definition. see docs for detail example.
* change GlobalCacheConfig.areaInCacheName default value to false (note: the default value was still `true` due to a bug, fixed in 2.8.0)

## 2.6.0
* GET/GET_ALL method of RefreshCache will not trigger auto refresh
* lettuce 4 is not supported
* jedis 2.9 is not supported
## 2.5.0
* ClassCastException may occurs when upgrade directly from versions <=2.3.3 and MultiLevelCache(or cacheType=CacheType.BOTH) is used. To solve this problem, upgrade to 2.4.4 and deploy it to product env first, then upgrade to 2.5.0 or above.
* Annotations on sub classes will override annotations on interfaces and super class.