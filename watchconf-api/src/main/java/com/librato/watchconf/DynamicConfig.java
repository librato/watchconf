package com.librato.watchconf;

import com.google.common.base.Optional;

/**
 * A base interface for a watchable configuration.
 *
 * @param <T> The type of configuration referred to by this DynamicConfig.
 * @author Ray Jenkins
 */
public interface DynamicConfig<T> {

    /**
     * A base interface for notification of configuration change.
     *
     * @param <T> The type of configuration referred to by this ChangeListener
     */
    public interface ChangeListener<T> {
        public void onChange(Optional<T> t);
        public void onError(Exception ex);
    }

    /**
     * Retrieve the configuration
     *
     * @return A reference to the configuration of type T.
     * @throws Exception cannot retrieve instance of Optional T
     */
    Optional<T> get() throws Exception;

    /**
     * Register a {@link com.librato.watchconf.DynamicConfig.ChangeListener} to be notified when configuration changes.
     *
     * @param changeListener {@link com.librato.watchconf.DynamicConfig.ChangeListener} to register.
     * @throws Exception cannot register listener
     */
    void registerListener(ChangeListener changeListener) throws Exception;

    /**
     * Remove a {@link com.librato.watchconf.DynamicConfig.ChangeListener}
     *
     * @param changeListener {@link com.librato.watchconf.DynamicConfig.ChangeListener} to remove.
     */
    void removeListener(ChangeListener changeListener);

    /**
     * Shutdown a dynamic configuration
     */
    void shutdown() throws Exception;
}
