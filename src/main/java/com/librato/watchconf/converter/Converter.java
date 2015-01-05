package com.librato.watchconf.converter;


/**
 * A base interface for serializing and de-serializing objects from to and front a byte[]
 * @param <T> The type of object this converter converts.
 */
public interface Converter<T> {

    /**
     * Converts a byte[] to an instance of <T>
     *
     * @param bytes The serialized object in bytes.
     * @param clazz The class of type T
     * @return a deserialized instance of <T>
     * @throws Exception
     */
    T toDomain(byte[] bytes, Class<T> clazz) throws Exception;

    /**
     * Converts an instance of <T> to a byte[]
     *
     * @param t and instance of type <T>
     * @return the serialized byte[] of t
     * @throws Exception
     */
    byte[] fromDomain(T t) throws Exception;
}
