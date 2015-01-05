package com.librato.watchconf;

import com.google.common.base.Optional;

/**
 * A base interface for a watchable configuration.
 * @author Ray Jenkins
 * @param <T> The type of configuration referred to by this DynamicConfig.
 */
public interface DynamicConfig<T> {

    /**
     * A base interface for notification of configuration change.
     * @param <T> The type of configuration referred to by this ChangeListener
     */
    public interface ChangeListener<T> {
        public void changed(Optional<T> t);
    }

    /**
     * Retrieve the configuration
     *
     * @return A reference to the configuration of type <T>.
     * @throws Exception
     */
    Optional<T> get() throws Exception;

    /**
     * Register a {@link com.librato.watchconf.DynamicConfig.ChangeListener} to be notified when configuration changes.
     *
     * @param changeListener {@link com.librato.watchconf.DynamicConfig.ChangeListener} to register.
     * @throws Exception
     */
    void registerListener(ChangeListener changeListener) throws Exception;

    /**
     * Remove a {@link com.librato.watchconf.DynamicConfig.ChangeListener}
     * @param changeListener {@link com.librato.watchconf.DynamicConfig.ChangeListener} to remove.
     */
    void removeListener(ChangeListener changeListener);
}
