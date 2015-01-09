package com.librato.watchconf.adapter.zookeeper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.adapter.AbstractConfigAdapter;
import com.librato.watchconf.converter.Converter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;

public class DynamicConfigZKAdapter<T> extends AbstractConfigAdapter<T, byte[]> {

    private final NodeCacheListener nodeCacheListener;
    private final NodeCache nodeCache;

    public DynamicConfigZKAdapter(final String path, final CuratorFramework curatorFramework, Converter<T, byte[]> converter, ChangeListener<T> changeListener) throws Exception {
        super(converter, Optional.fromNullable(changeListener));
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");
        Preconditions.checkNotNull(curatorFramework, "CuratorFramework cannot be null");

        if (curatorFramework.checkExists().forPath(path) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
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
