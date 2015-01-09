Overview
=========

Most of the time if you need to make a configuration change to a service you can modify a file, deploy and perform a rolling restart across the cluster. But sometimes you would rather not restart, watchconf aims to address those instances.

Watchconf provides a simple interface and several adapters for monitoring data from various sources, so when your configuration changes you're notified.

The primary interface for wathconf is DynamicConfig<T>

```java
/**
 * A base interface for a watchable configuration.
 *
 * @param <T> The type of configuration referred to by this DynamicConfig.
 * @author Ray Jenkins
 */
public interface DynamicConfig<T> {

    /**
     * A base interface for notification of configuration change.
     *
     * @param <T> The type of configuration referred to by this ChangeListener
     */
    public interface ChangeListener<T> {
        public void changed(Optional<T> t);
    }

    /**
     * Retrieve the configuration
     *
     * @return A reference to the configuration of type T.
     * @throws Exception cannot retrieve instance of Optional T
     */
    Optional<T> get() throws Exception;

    /**
     * Register a {@link com.librato.watchconf.DynamicConfig.ChangeListener} to be notified when configuration changes.
     *
     * @param changeListener {@link com.librato.watchconf.DynamicConfig.ChangeListener} to register.
     * @throws Exception cannot register listener
     */
    void registerListener(ChangeListener changeListener) throws Exception;

    /**
     * Remove a {@link com.librato.watchconf.DynamicConfig.ChangeListener}
     *
     * @param changeListener {@link com.librato.watchconf.DynamicConfig.ChangeListener} to remove.
     */
    void removeListener(ChangeListener changeListener);

    /**
     * Shutdown a dynamic configuration
     */
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


