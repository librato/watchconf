package com.librato.watchconf.adapter.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.librato.ExampleConfig;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.converter.JsonConverter;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.*;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import static junit.framework.TestCase.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicConfigZKAdapterTest {

    private class ExampleConfigAdapter extends DynamicConfigZKAdapter<ExampleConfig> {

        public ExampleConfigAdapter(CuratorFramework curatorFramework, ChangeListener<ExampleConfig> changeListener) throws Exception {
            super(ExampleConfig.class, "/test/config", curatorFramework, new JsonConverter<ExampleConfig>(), changeListener);
        }
    }

    @Test
    public void testInitialize() throws Exception {
        Stat stat = new Stat();
        CuratorFramework curatorFramework = mockFramework();
        GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
        WatchPathable watchPathable = mock(WatchPathable.class);
        Pathable pathable = mock(Pathable.class);
        GetDataWatchBackgroundStatable getDataWatchBackgroundStatable = mock(GetDataWatchBackgroundStatable.class);

        when(curatorFramework.getZookeeperClient()).thenReturn(mock(CuratorZookeeperClient.class));
        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(curatorFramework.getState()).thenReturn(CuratorFrameworkState.STARTED);
        when(getDataBuilder.storingStatIn(any(Stat.class))).thenReturn(watchPathable);
        when(getDataBuilder.decompressed()).thenReturn(getDataWatchBackgroundStatable);
        when(getDataWatchBackgroundStatable.storingStatIn(any(Stat.class))).thenReturn(watchPathable);
        when(watchPathable.usingWatcher(any(CuratorWatcher.class))).thenReturn(pathable);
        when(pathable.forPath(anyString())).thenReturn(new byte[0]);
        ExistsBuilder existsBuilder = mock(ExistsBuilder.class);
        EnsurePath ensurePath = mock(EnsurePath.class);
        when(curatorFramework.checkExists()).thenReturn(existsBuilder);
        when(curatorFramework.newNamespaceAwareEnsurePath(anyString())).thenReturn(ensurePath);
        when(ensurePath.excludingLast()).thenReturn(ensurePath);
        when(existsBuilder.forPath("/test/config")).thenReturn(stat);
        when(existsBuilder.usingWatcher(any(CuratorWatcher.class))).thenReturn(existsBuilder);
        when(existsBuilder.inBackground(any(BackgroundCallback.class))).thenReturn(existsBuilder);
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(curatorFramework, null);
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
        SyncBuilder syncBuilder = mock(SyncBuilder.class);
        GetDataWatchBackgroundStatable getDataWatchBackgroundStatable = mock(GetDataWatchBackgroundStatable.class);

        Pathable pathable = mock(Pathable.class);
        when(getDataWatchBackgroundStatable.storingStatIn(any(Stat.class))).thenReturn(watchPathable);
        when(curatorFramework.getZookeeperClient()).thenReturn(mock(CuratorZookeeperClient.class));
        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(curatorFramework.getState()).thenReturn(CuratorFrameworkState.STARTED);
        when(syncBuilder.inBackground(any(BackgroundCallback.class))).thenReturn(pathable);
        when(getDataBuilder.storingStatIn(any(Stat.class))).thenReturn(watchPathable);
        when(getDataBuilder.decompressed()).thenReturn(getDataWatchBackgroundStatable);
        when(watchPathable.usingWatcher(any(CuratorWatcher.class))).thenReturn(pathable);
        when(pathable.forPath(anyString())).thenReturn(objectMapper.writeValueAsBytes(exampleConfig));
        when(watchPathable.forPath(anyString())).thenReturn(objectMapper.writeValueAsBytes(exampleConfig));
        ExistsBuilder existsBuilder = mock(ExistsBuilder.class);
        EnsurePath ensurePath = mock(EnsurePath.class);
        when(curatorFramework.checkExists()).thenReturn(existsBuilder);
        when(curatorFramework.newNamespaceAwareEnsurePath(anyString())).thenReturn(ensurePath);
        when(ensurePath.excludingLast()).thenReturn(ensurePath);
        when(existsBuilder.forPath("/test/config")).thenReturn(stat);
        when(existsBuilder.usingWatcher(any(CuratorWatcher.class))).thenReturn(existsBuilder);
        when(existsBuilder.inBackground(any(BackgroundCallback.class))).thenReturn(existsBuilder);
        new ExampleConfigAdapter(curatorFramework, new DynamicConfig.ChangeListener<ExampleConfig>() {
            @Override
            public void onChange(Optional<ExampleConfig> t) {
                assertTrue(t.isPresent());
                assertEquals(t.get().name, "ray");
            }

            @Override
            public void onError(Exception ex) {

            }
        });
    }

    private CuratorFramework mockFramework() {
        CuratorFramework framework = mock(CuratorFramework.class);
        when(framework.getConnectionStateListenable()).thenReturn(mock(Listenable.class));
        return framework;
    }
}
