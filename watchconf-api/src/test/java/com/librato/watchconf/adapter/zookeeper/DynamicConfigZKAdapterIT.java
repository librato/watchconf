package com.librato.watchconf.adapter.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.librato.ExampleConfig;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.converter.JsonConverter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class DynamicConfigZKAdapterIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private class ExampleConfigAdapter extends DynamicConfigZKAdapter<ExampleConfig> {

        public ExampleConfigAdapter(CuratorFramework curatorFramework) throws Exception {
            super(ExampleConfig.class, "/watchconf/test/config", curatorFramework, new JsonConverter<ExampleConfig>());
        }

        public ExampleConfigAdapter(CuratorFramework curatorFramework, ChangeListener<ExampleConfig> changeListener) throws Exception {
            super(ExampleConfig.class, "/watchconf/test/config", curatorFramework, new JsonConverter<ExampleConfig>(), changeListener);
        }
    }

    @Before
    public void setUp() throws Exception {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();

        if (framework.checkExists().forPath("/watchconf/test/config") != null) {
            framework.delete().deletingChildrenIfNeeded().forPath("/watchconf");
        }
    }

    @Test
    public void initializationTest() throws Exception {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();

        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(framework);
        assertNotNull(exampleConfigAdapter);
    }

    @Test
    public void readConfigTest() throws Exception {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();

        ExampleConfig exampleConfig = new ExampleConfig();
        exampleConfig.name = "test123";

        framework.create().creatingParentsIfNeeded().forPath("/watchconf/test/config", objectMapper.writeValueAsBytes(exampleConfig));

        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(framework);
        exampleConfigAdapter.start();
        Optional<ExampleConfig> fetchedConfig = exampleConfigAdapter.get();
        assertTrue(fetchedConfig.isPresent());
        assertEquals("test123", fetchedConfig.get().name);
    }

    @Test
    public void notifyChangeTest() throws Exception {
        final CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();

        final ExampleConfig exampleConfig = new ExampleConfig();
        exampleConfig.name = "test123";

        framework.create().creatingParentsIfNeeded().forPath("/watchconf/test/config", objectMapper.writeValueAsBytes(exampleConfig));

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(framework, new DynamicConfig.ChangeListener<ExampleConfig>() {
            public void onChange(Optional<ExampleConfig> t) {
                countDownLatch.countDown();
            }

            public void onError(Exception ex) {

            }
        });

        exampleConfigAdapter.start();

        Executors.newSingleThreadScheduledExecutor().schedule(new Callable<Void>() {
            public Void call() throws Exception {
                exampleConfig.name = "updated";
                framework.setData().forPath("/watchconf/test/config", objectMapper.writeValueAsBytes(exampleConfig));
                return null;
            }
        }, 3, TimeUnit.SECONDS);
        countDownLatch.await(20, TimeUnit.SECONDS);
        assertEquals(0, countDownLatch.getCount());
        Optional<ExampleConfig> fetchedConfig = exampleConfigAdapter.get();
        assertTrue(fetchedConfig.isPresent());
        assertEquals("updated", fetchedConfig.get().name);
    }
}
