package com.librato.watchconf.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.listen.ListenerContainer;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WatchConfZKCache implements Closeable {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final CuratorFramework client;
    private final String path;
    private final EnsurePath ensurePath;
    private final AtomicReference<byte[]> data = new AtomicReference<byte[]>(null);
    private final AtomicReference<State> state = new AtomicReference<State>(State.LATENT);
    private final ListenerContainer<NodeCacheListener> listeners = new ListenerContainer<NodeCacheListener>();
    private final AtomicBoolean isConnected = new AtomicBoolean(true);
    private final ConnectionStateListener connectionStateListener = new ConnectionStateListener() {
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            if ((newState == ConnectionState.CONNECTED) || (newState == ConnectionState.RECONNECTED)) {
                if (isConnected.compareAndSet(false, true)) {
                    try {
                        internalRebuild();
                    } catch (Exception e) {
                        log.error("Trying to reset after reconnection", e);
                    }
                }
            } else {
                isConnected.set(false);
            }
        }
    };

    private final CuratorWatcher watcher = new CuratorWatcher() {
        @Override
        public void process(WatchedEvent event) throws Exception {
            reset();
        }
    };

    private enum State {
        LATENT,
        STARTED,
        CLOSED
    }


    private final BackgroundCallback backgroundCallback = new BackgroundCallback() {
        @Override
        public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
            processBackgroundResult(event);
        }
    };

    private void reset() throws Exception {
        if ((state.get() == State.STARTED) && isConnected.get()) {
            client.checkExists().usingWatcher(watcher).inBackground(backgroundCallback).forPath(path);
        }
    }

    /**
     * @param client curator client
     * @param path   the full path to the node to cache
     */
    public WatchConfZKCache(CuratorFramework client, String path) {
        this.client = client;
        this.path = path;
        ensurePath = client.newNamespaceAwareEnsurePath(path).excludingLast();
    }

    /**
     * Same as {@link #start()} but gives the option of doing an initial build
     *
     * @throws Exception errors
     */

    public void start() throws Exception {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTED), "Cannot be started more than once");
        ensurePath.ensure(client.getZookeeperClient());
        client.getConnectionStateListenable().addListener(connectionStateListener);
        internalRebuild();
    }

    @Override
    public void close() throws IOException {
        if (state.compareAndSet(State.STARTED, State.CLOSED)) {
            listeners.clear();
        }
        client.getConnectionStateListenable().removeListener(connectionStateListener);
    }

    /**
     * Return the cache listenable
     *
     * @return listenable
     */
    public ListenerContainer<NodeCacheListener> getListenable() {
        Preconditions.checkState(state.get() != State.CLOSED, "Closed");

        return listeners;
    }

    /**
     * NOTE: this is a BLOCKING method. Completely rebuild the internal cache by querying
     * for all needed data WITHOUT generating any events to send to listeners.
     *
     * @throws Exception errors
     */
    public void rebuild() throws Exception {
        Preconditions.checkState(state.get() == State.STARTED, "Not started");
        internalRebuild();
    }

    /**
     * Return the current data. There are no guarantees of accuracy. This is
     * merely the most recent view of the data. If the node does not exist,
     * this returns null
     *
     * @return data or null
     */
    public byte[] getCurrentData() {
        return data.get();
    }

    @VisibleForTesting
    volatile Exchanger<Object> rebuildTestExchanger;

    private void internalRebuild() throws Exception {
        if ((state.get() == State.STARTED) && isConnected.get()) {
            try {
                Stat stat = new Stat();
                byte[] data = client.getData().storingStatIn(stat).usingWatcher(watcher).forPath(path);
                setNewData(data);
            } catch (KeeperException.NoNodeException e) {
                data.set(null);
            }
        }
    }

    private void processBackgroundResult(CuratorEvent event) throws Exception {
        switch (event.getType()) {
            case GET_DATA: {
                if (event.getResultCode() == KeeperException.Code.OK.intValue()) {
                    setNewData(event.getData());
                }
                break;
            }

            case EXISTS: {
                if (event.getResultCode() == KeeperException.Code.NONODE.intValue()) {
                    setNewData(null);
                } else if (event.getResultCode() == KeeperException.Code.OK.intValue()) {
                    client.getData().usingWatcher(watcher).inBackground(backgroundCallback).forPath(path);
                }
                break;
            }
        }
    }

    private void setNewData(byte[] newData) throws InterruptedException {
        byte[] previousData = data.getAndSet(newData);
        if (!Objects.equal(previousData, newData)) {
            listeners.forEach
                    (
                            new Function<NodeCacheListener, Void>() {
                                @Override
                                public Void apply(NodeCacheListener listener) {
                                    try {
                                        listener.nodeChanged();
                                    } catch (Exception e) {
                                        log.error("Calling listener", e);
                                    }
                                    return null;
                                }
                            }
                    );

            if (rebuildTestExchanger != null) {
                try {
                    rebuildTestExchanger.exchange(new Object());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}

