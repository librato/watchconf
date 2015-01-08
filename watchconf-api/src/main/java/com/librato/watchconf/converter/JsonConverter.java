package com.librato.watchconf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * An implementation of {@link com.librato.watchconf.converter.Converter} that serializes and de-serializes
 * JSON/POJOs
 * @param <T> the type referenced by this converter
 */
public class JsonConverter<T> implements Converter<T, byte[]> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts an byte[] of JSON to an instance of the type referenced by this class.
     * @param bytes serialized json in bytes
     * @param clazz the class of the type referenced by this class.
     * @return an instance of type T
     * @throws IOException cannot convert to type T
     */
    @Override
    public T toDomain(byte[] bytes, Class<T> clazz) throws IOException {
        return objectMapper.readValue(bytes, clazz);
    }

    /**
     * Converts and instance of type T to JSON serialized byte[]
     * @param t an instance of type T
     * @return bytes of serialized JSON for t
     * @throws JsonProcessingException cannot convert to byte[]
     */
    @Override
    public byte[] fromDomain(T t) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(t);
    }
}
