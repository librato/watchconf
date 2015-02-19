package com.librato.watchconf.adapter.zookeeper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.adapter.AbstractConfigAdapter;
import com.librato.watchconf.adapter.WatchConfZKCache;
import com.librato.watchconf.converter.Converter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DynamicConfigZKAdapter<T> extends AbstractConfigAdapter<T, byte[]> {

    private Logger log = LoggerFactory.getLogger(DynamicConfig.class);
    private NodeCacheListener nodeCacheListener;
    private WatchConfZKCache watchConfZKCache;

    public DynamicConfigZKAdapter(final Class<T> clazz,
                                  final String path,
                                  final CuratorFramework curatorFramework,
                                  Converter<T, byte[]> converter,
                                  ChangeListener<T> changeListener) throws Exception {
        super(clazz, converter, Optional.fromNullable(changeListener));
        Preconditions.checkNotNull(curatorFramework, "CuratorFramework cannot be null");
        Preconditions.checkArgument(curatorFramework.getState() == CuratorFrameworkState.STARTED, "CuratorFramework must be started");
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");

        if (curatorFramework.checkExists().forPath(path) == null) {
            try {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path, "{}".getBytes());
            } catch (KeeperException.NodeExistsException ex) {
                log.info("Node exists on create, continuing");
            }
        }

        this.watchConfZKCache = new WatchConfZKCache(curatorFramework, path);
        this.nodeCacheListener = new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                getAndSet(watchConfZKCache.getCurrentData());
                notifyListeners();
            }
        };

        this.watchConfZKCache.getListenable().addListener(nodeCacheListener);
        this.watchConfZKCache.start();
    }

    public DynamicConfigZKAdapter(Class<T> clazz, String path, CuratorFramework curatorFramework, Converter converter) throws Exception {
        this(clazz, path, curatorFramework, converter, null);
    }
}
