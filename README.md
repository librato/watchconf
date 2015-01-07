Overview
=========

Most of the time if you need to make a configuration change to a service you can modify a file, deploy and perform perform a rolling restart across the cluster. But sometimes you shouldn't, can't or would rather not restart, watchconf aims to address those instances.

Watchconf provides a simple interface and several adapters for monitoring data from various sources, so when your configuration changes you're notified.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.librato.watchconf</groupId>
  <artifactId>watchconf-api</artifactId>
  <version>0.0.3</version>
</dependency>
```
