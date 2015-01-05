package com.librato.watchconf.zookeeper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.converter.Converter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.log4j.Logger;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DynamicConfigZKAdapter<T> implements DynamicConfig<T> {

    private static final Logger log = Logger.getLogger(DynamicConfigZKAdapter.class);
    private final String path;
    private final CuratorFramework curatorFramework;
    private final NodeCacheListener nodeCacheListener;
    private final Class<T> clazz;
    private NodeCache nodeCache;
    private final List<ChangeListener> changeListenerList = new ArrayList();
    private AtomicReference<Optional<T>> config = new AtomicReference(Optional.absent());
    private final Converter<T> converter;

    public DynamicConfigZKAdapter(String path, CuratorFramework curatorFramework, Converter converter, ChangeListener changeListener) throws Exception {
        this(path, curatorFramework, converter);
        registerListener(changeListener);
    }

    public DynamicConfigZKAdapter(String path, CuratorFramework curatorFramework, Converter converter) throws Exception {
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");
        Preconditions.checkNotNull(curatorFramework, "CuratorFramework cannot be null");
        this.path = path;
        this.curatorFramework = curatorFramework;
        this.converter = converter;

        if (curatorFramework.checkExists().forPath(path) == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
        }

        this.clazz = getClassForType();

        getAndSet();

        this.nodeCache = new NodeCache(curatorFramework, path);
        this.nodeCacheListener = new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                getAndSet();
                Optional<T> c = config.get();
                for (ChangeListener changeListener : changeListenerList) {
                    changeListener.changed(c);
                }

            }
        };

        this.nodeCache.getListenable().addListener(nodeCacheListener);
        this.nodeCache.start(true);
    }

    public Optional<T> get() throws Exception {
        return config.get();
    }

    public void registerListener(ChangeListener changeListener) throws Exception {
        changeListenerList.add(changeListener);
    }

    public void removeListener(ChangeListener changeListener) {
        changeListenerList.remove(changeListener);
    }

    private Class<T> getClassForType() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    private void getAndSet() {
        try {
            config.set(Optional.of(converter.toDomain(curatorFramework.getData().forPath(path), clazz)));
        } catch (Exception ex) {
            log.error("unable to parse config", ex);
            config.set(Optional.<T>absent());
        }
    }
}
