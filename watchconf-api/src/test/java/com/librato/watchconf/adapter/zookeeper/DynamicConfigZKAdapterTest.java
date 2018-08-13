package com.librato.watchconf.adapter.zookeeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librato.ExampleConfig;
import com.librato.watchconf.converter.JsonConverter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class DynamicConfigZKAdapterTest {
    private static final String TEST_PATH = "/test/config";
    private CuratorFramework curatorFramework;

    private class ExampleConfigAdapter extends DynamicConfigZKAdapter<ExampleConfig> {
        ExampleConfigAdapter(CuratorFramework curatorFramework) throws Exception {
            super(ExampleConfig.class, TEST_PATH, curatorFramework, new JsonConverter<ExampleConfig>());
        }
    }

    @Before
    public void before() throws Exception {
        TestingServer server = new TestingServer();
        curatorFramework = CuratorFrameworkFactory.newClient(server.getConnectString(), new RetryOneTime(1));
        curatorFramework.start();
        curatorFramework.create().creatingParentContainersIfNeeded().forPath(TEST_PATH, getExampleBytes());
    }

    private byte[] getExampleBytes() throws JsonProcessingException {
        ExampleConfig exampleConfig = new ExampleConfig();
        exampleConfig.name = "ray";

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(exampleConfig);
    }

    @Test
    public void testInit() throws Exception {
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(curatorFramework);
        assertNotNull(exampleConfigAdapter);
    }

    @Test
    public void testRead() throws Exception {
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(curatorFramework);
        exampleConfigAdapter.start();
        assertNotNull(exampleConfigAdapter);
        assertTrue(exampleConfigAdapter.get().isPresent());
        assertEquals(exampleConfigAdapter.get().get().name, "ray");
    }
}
