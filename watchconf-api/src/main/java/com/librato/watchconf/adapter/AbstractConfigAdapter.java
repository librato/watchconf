package com.librato.watchconf.adapter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.converter.Converter;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractConfigAdapter<T, V> implements DynamicConfig<T> {

    protected final Class<T> clazz;
    protected final List<ChangeListener> changeListenerList = new ArrayList();
    protected final AtomicReference<Optional<T>> config = new AtomicReference(Optional.absent());
    protected final Converter<T, V> converter;

    protected AbstractConfigAdapter(Converter<T, V> converter, Optional<ChangeListener<T>> changeListener) {
        this(converter);
        if (changeListener.isPresent()) {
            registerListener(changeListener.get());
        }
    }

    protected AbstractConfigAdapter(Converter<T, V> converter) {
        Preconditions.checkNotNull(converter, "converter cannot be null");
        this.converter = converter;
        this.clazz = getClassForType();
    }

    public Optional<T> get() throws Exception {
        return config.get();
    }

    public void registerListener(ChangeListener changeListener) {
        changeListenerList.add(changeListener);
    }

    public void removeListener(ChangeListener changeListener) {
        changeListenerList.remove(changeListener);
    }

    protected void notifyListeners() {
        for (ChangeListener changeListener : changeListenerList) {
            changeListener.changed(config.get());
        }
    }


    private Class<T> getClassForType() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }


}
