Overview
=========

Most of the time if you need to make a configuration change to a service you can modify a file, deploy, and perform a rolling restart across the cluster. But sometimes you would rather not restart. Watchconf aims to address these occasions.

Watchconf provides a simple read-only interface and several adapters for monitoring data from various sources, so that you're notified when configurations change.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.librato.watchconf</groupId>
  <artifactId>watchconf-api</artifactId>
  <version>0.0.7</version>
</dependency>
```

## Getting Started

The primary interface for wathconf is ```DynamicConfig<T>```:

```java
public interface DynamicConfig<T> {
    
    public interface ChangeListener<T> {
        public void onChange(Optional<T> t);
        public void onError(Exception ex);
    }

    Optional<T> get() throws Exception;
    void registerListener(ChangeListener changeListener) throws Exception;
    void removeListener(ChangeListener changeListener);
    void shutdown() throws Exception;
}
```

### Adapters

Watchconf provides abstract adapter implementations for each source supported by watchconf. To create your ```DynamicConfig``` object simply extend the appropriate adapter, select your converter type, and instantiate.

* [Zookeeper](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/zookeeper/DynamicConfigZKAdapter.java) - used in production at Librato
* [Redis](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/redis/DynamicConfigRedisAdapter.java)
* [File](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/adapter/file/DynamicConfigFileAdapter.java)

### Converters

Adapters use the ```Converter``` interface to convert serialized configuration into objects. Watchconf provides converters for various data formats including [JSON](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/converter/JsonConverter.java) and [YAML](https://github.com/librato/watchconf/blob/master/watchconf-api/src/main/java/com/librato/watchconf/converter/YAMLConverter.java). If you need to support another format simply implement a converter.

```java
public interface Converter<T, V> {
    T toDomain(V v, Class<T> clazz) throws Exception;
    V fromDomain(T t) throws Exception;
}
```

### ChangeListener

```DynamicConfig``` allows you to register a ```ChangeListener``` to be notified when your configuration changes. If you prefer not to be notified and would rather poll you can use the ```Optional<T> get()``` method.

```java
public interface ChangeListener<T> {
  public void onChange(Optional<T> t);
  public void onError(Exception ex);
}
```

# Example Usage

At Librato we're using [Zookeeper](http://zookeeper.apache.org/) to store configuration that we want to update on the fly and have watchconf notify our service. One place it's particularly useful is controlling the Kafka producers in our streaming teir. Let's say we're storing our configuration in JSON in a znode named `/services/kafka/config`. First we need to create a POJO representation of our config, in this case we have one named ```KafkaConfig```.
```java
public class KafkaConfig {
    public List<Broker> defaultBrokers;
    public List<Topic> topics = new ArrayList();

    public static class Topic {
        public String name;
        public List<Broker> brokers = new ArrayList();
    }
    
    public static class Broker {
        public int id;
        public String ipAddress;
        public int port;
        public boolean produce = true;
    }
}
```
With this configuration we can define a default set of brokers for our service (and denote whether they are eligible for receiving data from our producers. We can also override our broker configuration on a per topic basis, sending output from different streaming topologies into different brokers.

In order to watch our ```KafkaConfig``` we first need a ```CuratorFramework``` instance. We then extend the Zookeeper adapter and use the ```JsonConverter```.

```java
 public static class KafkaConfigAdapter extends DynamicConfigZKAdapter<KafkaConfig> 
   implements ChangeListener<KafkaConfig> {
    
    public KafkaConfigAdapter(CuratorFramework curatorFramework) throws Exception {
      super("/services/kafka/config", curatorFramework, new JsonConverter<KafkaConfig>());
    }
    
    public void onChange(Optional<KafkaConfig> config) {
      doSomething(config);
    }
 }

 CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();
  
 DynamicConfig<KafkaConfig> config = new KafkaConfigAdapter(framework);
```

# Testing

Unit tests can be run with ```mvn test```. In addition there are integration tests (ending in *IT.java). Those may be run with ```mvn clean; mvn verify```, though you need to have both Zookeeper and Redis installed.

# Operational Concerns

Upon initial instantiatation of an adapter, if there are errors parsing a configuration or if the resource is non-existant, the ```Optional<T> get()``` method of ```DynamicConfig``` will return a ```Optional.absent()```. If during operation configuration changes are made and errors are encountered, parsing the updated configuration a log message will be written ```log.error("unable to parse config", ex);``` and any ChangeListeners will be notified, but the previous configuration will still be returned in calls to ```Optional<T> get()```. This is by design as we wish to avoid impacting a running service due to a configuration change error.

## F.A.Q.

* What happens if I push a bad configuation that causes an error during parse? Will it break my service?

  No, If you push a bad configuration (unparsable) you will most likely see jackson parsing exceptions but your service will continue to run using the last known good configuration. 
  
  ```
  java.lang.RuntimeException: java.lang.RuntimeException: com.fasterxml.jackson.core.JsonParseException: Unexpected character ('.' (code 46)): Expected space separating root-level values
```

 Simply push a new configuration with watchconf to resolve the problem. You should also see log messages from the watchconf AbstractConfigAdapter and calls to onError in the watchconf adapter in amnis.
 
 * ```2015-01-16 19:42:28 c.l.w.a.AbstractConfigAdapter [ERROR] unable to parse config```
 * ```c.l.a.KafkaJsonWriterBolt$1.onError(KafkaJsonWriterBolt.java:79) ~[stormjar.jar:na]```

* What happens if I accidentally delete a the znode for a config?

  Nothing, your services will continue to run fine, just push a new configuration and they will pick up the changes.
  
* What happens if the zookeeper connection fails?

  Your services will continue to run on zookeeper connection failure, they will use the last known good configuration. You will see curator and zookeeper connection errors in the log. Once connectivity is restored the error messages will subside.

## Watchconf-util

The watchconf-util package comes with a utility for parsing and pushing configuration into zookeeper. At librato we keep configuration in YAML stored in a repo. If I want to push changes to a cluster I would update the YAML, push to our repo and deploy to Zookeeper. To run the configuration push utility enter

```java -jar ./target/watchconf-util-0.0.7-SNAPSHOT.jar```

You will be prompted to supply arguments for 5 flags.

```
watchconf: Must specify -zkServer <host:port> and additional required flags
-format [yaml|json]: input file format
-f <file>: input file to read from
-o [yaml|json]: format of data to output to znode
-z: full path to znode to update, will create parents and node doesn't exist
```

