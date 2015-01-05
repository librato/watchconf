package com.librato.watchconf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonConverter<T> implements Converter<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public T toDomain(byte[] bytes, Class<T> clazz) throws IOException {
        return objectMapper.readValue(bytes, clazz);
    }

    @Override
    public byte[] fromDomain(T t) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(t);
    }
}
