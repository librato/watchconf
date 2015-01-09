package com.librato.watchconf.adapter.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librato.ExampleConfig;
import com.librato.watchconf.converter.JsonConverter;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.*;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicConfigZKAdapterTest {

    private class ExampleConfigAdapter extends DynamicConfigZKAdapter<ExampleConfig> {

        public ExampleConfigAdapter(CuratorFramework curatorFramework) throws Exception {
            super("/test/config", curatorFramework, new JsonConverter<ExampleConfig>());
        }
    }

    @Test
    public void testInitialize() throws Exception {
        Stat stat = new Stat();
        CuratorFramework curatorFramework = mockFramework();
        GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
        WatchPathable watchPathable = mock(WatchPathable.class);
        when(curatorFramework.getZookeeperClient()).thenReturn(mock(CuratorZookeeperClient.class));
        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(getDataBuilder.storingStatIn(any(Stat.class))).thenReturn(watchPathable);
        when(watchPathable.forPath(anyString())).thenReturn(new byte[0]);
        when(getDataBuilder.forPath(anyString())).thenReturn(null);
        ExistsBuilder existsBuilder = mock(ExistsBuilder.class);
        EnsurePath ensurePath = mock(EnsurePath.class);
        when(curatorFramework.checkExists()).thenReturn(existsBuilder);
        when(curatorFramework.newNamespaceAwareEnsurePath(anyString())).thenReturn(ensurePath);
        when(ensurePath.excludingLast()).thenReturn(ensurePath);
        when(existsBuilder.forPath("/test/config")).thenReturn(stat);
        when(existsBuilder.usingWatcher(any(CuratorWatcher.class))).thenReturn(existsBuilder);
        when(existsBuilder.inBackground(any(BackgroundCallback.class))).thenReturn(existsBuilder);
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(curatorFramework);
        assertNotNull(exampleConfigAdapter);
    }

    @Test
    public void testReadConfig() throws Exception {

        ExampleConfig exampleConfig = new ExampleConfig();
        exampleConfig.name = "ray";
        ObjectMapper objectMapper = new ObjectMapper();

        Stat stat = new Stat();
        CuratorFramework curatorFramework = mockFramework();
        GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
        WatchPathable watchPathable = mock(WatchPathable.class);
        when(curatorFramework.getZookeeperClient()).thenReturn(mock(CuratorZookeeperClient.class));
        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(getDataBuilder.storingStatIn(any(Stat.class))).thenReturn(watchPathable);
        when(watchPathable.forPath(anyString())).thenReturn(objectMapper.writeValueAsBytes(exampleConfig));
        when(getDataBuilder.forPath(anyString())).thenReturn(objectMapper.writeValueAsBytes(exampleConfig));
        ExistsBuilder existsBuilder = mock(ExistsBuilder.class);
        EnsurePath ensurePath = mock(EnsurePath.class);
        when(curatorFramework.checkExists()).thenReturn(existsBuilder);
        when(curatorFramework.newNamespaceAwareEnsurePath(anyString())).thenReturn(ensurePath);
        when(ensurePath.excludingLast()).thenReturn(ensurePath);
        when(existsBuilder.forPath("/test/config")).thenReturn(stat);
        when(existsBuilder.usingWatcher(any(CuratorWatcher.class))).thenReturn(existsBuilder);
        when(existsBuilder.inBackground(any(BackgroundCallback.class))).thenReturn(existsBuilder);
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(curatorFramework);
        assertNotNull(exampleConfigAdapter);
        assertTrue(exampleConfigAdapter.get().isPresent());
        assertEquals(exampleConfigAdapter.get().get().name, "ray");
    }

    private CuratorFramework mockFramework() {
        CuratorFramework framework = mock(CuratorFramework.class);
        when(framework.getConnectionStateListenable()).thenReturn(mock(Listenable.class));
        return framework;
    }
}
