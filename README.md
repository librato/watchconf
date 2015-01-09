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

Getting Started
=========

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

# Adapters

Watchconf provides abstract adapter implementations for each source supported by watchconf, to create your DynamicConfig object simply extend the appropriate adapter, select your converter type and instantiate.

* [Zookeeper](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/zookeeper/DynamicConfigZKAdapter.java)
* [Redis](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/redis/DynamicConfigRedisAdapter.java)
* [File](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/file/DynamicConfigFileAdapter.java)

# Converters

Adapters use the ```Converter``` interface to convert serialized configuration into objects. Watchconf provides converter for various data formats including JSON and YAML. If you need to support another format simply implement a converter.

```java
public interface Converter<T, V> {

    T toDomain(V v, Class<T> clazz) throws Exception;
    V fromDomain(T t) throws Exception;
}
```
