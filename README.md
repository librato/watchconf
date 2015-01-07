Overview
=========

Most of the time if you need to make a configuration change to a service you can modify a file, deploy and perform perform a rolling restart across the cluster. But sometimes you shouldn't, can't or rather not restart to change configuration, watchconf aims to address those instances.


## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.librato.watchconf</groupId>
  <artifactId>watchconf-api</artifactId>
  <version>0.0.3</version>
</dependency>
```
