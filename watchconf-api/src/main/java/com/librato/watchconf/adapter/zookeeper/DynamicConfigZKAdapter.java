package com.librato.watchconf.adapter.zookeeper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.adapter.AbstractConfigAdapter;
import com.librato.watchconf.converter.Converter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DynamicConfigZKAdapter<T> extends AbstractConfigAdapter<T, byte[]> {

    private Logger log = LoggerFactory.getLogger(DynamicConfig.class);
    private NodeCacheListener nodeCacheListener;
    private NodeCache nodeCache;

    public DynamicConfigZKAdapter(final String path, final CuratorFramework curatorFramework, Converter<T, byte[]> converter, ChangeListener<T> changeListener) throws Exception {
        super(converter, Optional.fromNullable(changeListener));
        Preconditions.checkNotNull(curatorFramework, "CuratorFramework cannot be null");
        Preconditions.checkArgument(curatorFramework.getState() == CuratorFrameworkState.STARTED, "CuratorFramework must be started");
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");

        if (curatorFramework.checkExists().forPath(path) == null) {
            try {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path);
            } catch (KeeperException.NodeExistsException ex) {
                log.info("Node exists on create, continuing");
            }
        }

        getAndSet(curatorFramework.getData().forPath(path));

        this.nodeCache = new NodeCache(curatorFramework, path);
        this.nodeCacheListener = new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                getAndSet(curatorFramework.getData().forPath(path));
                notifyListeners();
            }
        };

        this.nodeCache.getListenable().addListener(nodeCacheListener);
        this.nodeCache.start(true);
    }

    public DynamicConfigZKAdapter(String path, CuratorFramework curatorFramework, Converter converter) throws Exception {
        this(path, curatorFramework, converter, null);
    }
}
