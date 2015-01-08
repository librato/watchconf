package com.librato.watchconf.adapter;

import com.google.common.base.Optional;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.converter.Converter;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractConfigAdapter<T> implements DynamicConfig<T> {

    protected final Class<T> clazz;
    protected final List<ChangeListener> changeListenerList = new ArrayList();
    protected final AtomicReference<Optional<T>> config = new AtomicReference(Optional.absent());
    protected final Converter<T> converter;

    protected AbstractConfigAdapter(Converter<T> converter) {
        this.converter = converter;
        this.clazz = getClassForType();
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
