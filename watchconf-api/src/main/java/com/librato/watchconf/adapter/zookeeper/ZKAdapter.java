package com.librato.watchconf.adapter.zookeeper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.adapter.AbstractConfigAdapter;
import com.librato.watchconf.converter.Converter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.log4j.Logger;

public abstract class ZKAdapter<T> extends AbstractConfigAdapter<T> {

    private static final Logger log = Logger.getLogger(ZKAdapter.class);
    private final String path;
    private final CuratorFramework curatorFramework;
    private final NodeCacheListener nodeCacheListener;
    private final NodeCache nodeCache;

    public ZKAdapter(String path, CuratorFramework curatorFramework, Converter<T> converter, ChangeListener<T> changeListener) throws Exception {
        super(converter, Optional.fromNullable(changeListener));
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");
        Preconditions.checkNotNull(curatorFramework, "CuratorFramework cannot be null");
        this.path = path;
        this.curatorFramework = curatorFramework;

        if (curatorFramework.checkExists().forPath(path) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
        }

        getAndSet();

        this.nodeCache = new NodeCache(curatorFramework, path);
        this.nodeCacheListener = new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                getAndSet();
                notifyListeners();
            }
        };

        this.nodeCache.getListenable().addListener(nodeCacheListener);
        this.nodeCache.start(true);
    }

    public ZKAdapter(String path, CuratorFramework curatorFramework, Converter converter) throws Exception {
        this(path, curatorFramework, converter, null);
    }

    private void getAndSet() {
        try {
            config.set(Optional.of(converter.toDomain(curatorFramework.getData().forPath(path), clazz)));
        } catch (Exception ex) {
            log.error("unable to parse config", ex);
        }
    }
}
