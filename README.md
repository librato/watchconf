Overview
=========

Most of the time if you need to make a configuration change to a service you can modify a file, deploy and perform a rolling restart across the cluster. But sometimes you would rather not restart, watchconf aims to address those instances.

Watchconf provides a simple interface and several adapters for monitoring data from various sources, so when your configuration changes you're notified.

The primary interface for wathconf is ```java DynamicConfig<T>``

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

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.librato.watchconf</groupId>
  <artifactId>watchconf-api</artifactId>
  <version>0.0.4</version>
</dependency>
```


