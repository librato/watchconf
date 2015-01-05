package com.librato.watchconf.converter;

public interface Converter<T> {

    T toDomain(byte[] bytes, Class<T> clazz) throws Exception;

    byte[] fromDomain(T t) throws Exception;
}
