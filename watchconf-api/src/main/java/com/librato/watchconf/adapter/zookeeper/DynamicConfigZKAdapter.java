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

    private final CuratorFramework curatorFramework;
    private final String path;
    private Logger log = LoggerFactory.getLogger(DynamicConfig.class);
    private NodeCacheListener nodeCacheListener;
    private NodeCache nodeCache;

    public DynamicConfigZKAdapter(final Class<T> clazz,
                                  final String path,
                                  final CuratorFramework curatorFramework,
                                  Converter<T, byte[]> converter,
                                  ChangeListener<T> changeListener) throws Exception {
        super(clazz, converter, Optional.fromNullable(changeListener));
        Preconditions.checkNotNull(curatorFramework, "CuratorFramework cannot be null");
        Preconditions.checkArgument(curatorFramework.getState() == CuratorFrameworkState.STARTED, "CuratorFramework must be started");
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");

        this.curatorFramework = curatorFramework;
        this.path = path;
        this.nodeCache = new NodeCache(curatorFramework, path);
    }

    public void start() throws Exception {
        started.set(true);
        if (curatorFramework.checkExists().forPath(path) == null) {
            try {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path, "{}".getBytes());
            } catch (KeeperException.NodeExistsException ex) {
                log.info("Node exists on create, continuing");
            }
        }

        this.nodeCacheListener = new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                notifyListeners(get());
            }
        };

        this.nodeCache.getListenable().addListener(nodeCacheListener);
        this.nodeCache.start(true);
    }

    public Optional<T> get() throws Exception {
        Preconditions.checkArgument(started.get(), "Adapter must be started before calling get");
        return Optional.of(converter.toDomain(nodeCache.getCurrentData().getData(), clazz));
    }

    public DynamicConfigZKAdapter(Class<T> clazz, String path, CuratorFramework curatorFramework, Converter converter) throws Exception {
        this(clazz, path, curatorFramework, converter, null);
    }
}
