Overview
=========

Most of the time if you need to make a configuration change to a service you can modify a file, deploy and perform a rolling restart across the cluster. But sometimes you would rather not restart, watchconf aims to address those instances.

Watchconf provides a simple read-only interface and several adapters for monitoring data from various sources, so when your configuration changes you're notified.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.librato.watchconf</groupId>
  <artifactId>watchconf-api</artifactId>
  <version>0.0.4</version>
</dependency>
```

## Getting Started

The primary interface for wathconf is ```DynamicConfig<T>```

```java
public interface DynamicConfig<T> {
    
    public interface ChangeListener<T> {
        public void changed(Optional<T> t);
    }

    Optional<T> get() throws Exception;
    void registerListener(ChangeListener changeListener) throws Exception;
    void removeListener(ChangeListener changeListener);
    void shutdown() throws Exception;
}
```

### Adapters

Watchconf provides abstract adapter implementations for each source supported by watchconf, to create your DynamicConfig object simply extend the appropriate adapter, select your converter type and instantiate.

* [Zookeeper](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/zookeeper/DynamicConfigZKAdapter.java)
* [Redis](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/redis/DynamicConfigRedisAdapter.java)
* [File](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/file/DynamicConfigFileAdapter.java)

### Converters

Adapters use the ```Converter``` interface to convert serialized configuration into objects. Watchconf provides converter for various data formats including [JSON](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/converter/JsonConverter.java) and [YAML](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/converter/YAMLConverter.java). If you need to support another format simply implement a converter.

```java
public interface Converter<T, V> {

    T toDomain(V v, Class<T> clazz) throws Exception;
    V fromDomain(T t) throws Exception;
}
```

### ChangeListener

```DynamicConfig``` allows you to register a ```ChangeListener``` to be notified when your configuration changes, if you prefer not to be notified and would rather poll you can use the ```Optional<T> get()``` method.

```java
public interface ChangeListener<T> {
  public void changed(Optional<T> t);
}
```

# Example Usage

At Librato we're using [Zookeeper](http://zookeeper.apache.org/) to store configuration that we want to update on the fly and have watchconf notify our service, let's say we have a clustered service named ```WebService``` and we're storing our configuration in JSON in a znode named /services/webservice/config. First we need to create a POJO representation of our config, we'll call it ```WebServiceConfig```

```java
public class WebServiceConfig {
  
  public int version;
  public String someUrl;
  
  public KafkaConfig kafkaConfig;
  
  public static class KafkaConfig {
    
    public int queueTimeMs;
    public int batchSize;
  }
}
```

In order to watch our ```WebServiceConfig``` we first need a ```CuratorFramework``` instance and then we extend the Zookeeper adapter and use the JsonConverter.

```java

 public static class WebServiceAdapter extends DynamicConfigZKAdapter<WebServiceConfig> 
   implements ChangeListener<WebServiceConfig> {
    
    public WebServiceAdapter(CuratorFramework curatorFramework) throws Exception {
      super("/services/webservice/config", curatorFramework, new JsonConverter<WebServiceConfig>());
    }
    
    public void changed(Optional<WebServiceConfig> config) {
      doSomething(config);
    }
 }

 CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();
  
 DynamicConfig<WebServiceConfig> config = new WebServiceAdapter(framwork);
```
