package com.librato.watchconf.converter;


/**
 * A base interface for serializing and de-serializing objects from to and front a byte[]
 * @param <T> The type of object this converter converts.
 */
public interface Converter<T, V> {

    /**
     * Converts a value of type V to an instance of T
     * @param v type value type
     * @param clazz The class of type T
     * @return a deserialized instance of T
     * @throws Exception unable to convert to T
     */
    T toDomain(V v, Class<T> clazz) throws Exception;

    /**
     * Converts an instance of T to a byte[]
     *
     * @param t and instance of type T
     * @return the serialized byte[] of t
     * @throws Exception unable to convert to bytes
     */
    V fromDomain(T t) throws Exception;
}
