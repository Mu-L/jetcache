# spring兼容性
jetcache在以下spring/spring-boot版本下通过了测试，如果你只用部分功能或者能自己调整依赖的的话，适用范围还可以更大一些。

| jetcache版本 | spring版本                    | spring boot版本               | 说明                                                                            |
|------------|-----------------------------|-----------------------------|-------------------------------------------------------------------------------|
| 2.5        | 4.0.8.RELEASE~5.1.1.RELEASE | 1.1.9.RELEASE~2.0.5.RELEASE ||
| 2.6        | 5.0.4.RELEASE~5.2.4.RELEASE | 2.0.0.RELEASE~2.2.5.RELEASE | jetcache-redis依赖jedis3.1.0，spring-data(jedis，boot版本<=2.1.X)依赖jedis2.9.3，不能同时用 |
| 2.7        | 5.2.4.RELEASE~5.3.23        | 2.2.5.RELEASE~2.7.5         | jetcahe-redis依赖jedis4，spring-data(jedis)依赖jedis3，不能同时用                        |
| 2.7.4+     | 5.2.4.RELEASE~6.2.18        | 2.2.5.RELEASE~3.5.14        | |

# 兼容性改动说明
## 2.8.0
* 不再支持kryo4，`com.esotericsoftware:kryo`已升级到5.x。kryo4序列化数据与kryo5不兼容，升级前需等待旧缓存过期或手动清空
* 移除了`IDENTITY_NUMBER_KRYO4`常量

## 2.7.4
* 默认传递依赖spring-boot 3.1.3，spring-framework 6.0.11，slf4j-api 2.x
* 移除了javax.annotation:javax.annotation-api这个依赖，如果你用了@PostConstruct等注解，可能需要自己加上这个依赖

## 2.7.2
* 更新了redisson的编码方式，和2.7.1不兼容

## 2.7.0
* jetcahe-redis依赖jedis4，如果你使用spring data并且使用jedis的话（spring-data默认用lettuce），它需要3，所以你需要自己把版本改回去，并且不能再使用jetcahe-redis了（改用jetcache-redis-springdata）
* encoder/decoder支持kryo5，在yml中"kryo"对应`com.esotericsoftware:kryo`（5.x），"kryo5"对应`com.esotericsoftware.kryo:kryo5`。两者的序列化格式兼容，但maven坐标和Java包名不同，可以并存
* lettuce连接redis cluster需要在yml里面指定mode=cluster
* 默认的key convertor改成了"fastjson2"，fastjson2和fastjson可以并存，fastjson（非fastjson2）/kryo/kryo5/mvel在maven中都改为optional，如果使用了需要用户手工声明依赖
* 如果没有使用spring boot，应该增加```@Import(JetCacheBaseBeans.class)```，同时删除原来定义的configProvider bean，具体例子可以看最新文档
* GlobalCacheConfig.areaInCacheName默认值改为false(但是有bug，默认值可能还是true)，areaInCacheName=false这个还得加上

## 2.6.0
* GET/GET_ALL方法不再触发自动刷新（大写的方法只简单访问缓存， 小写的方法才能触发这些附加功能）
* 不再支持lettuce4
* 不再支持jedis2.9

## 2.5.0
* 从2.3.3及更低版本升级到2.5.0会发生ClassCastException（如果你使用了MultiLevelCache或者cacheType.CacheType.BOTH）。
解决办法是先升级到2.4.4并且发布到生产环境，然后再升级到2.5.0。
* 子类的注解会覆盖接口和父类