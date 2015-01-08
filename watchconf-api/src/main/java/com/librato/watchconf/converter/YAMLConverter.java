package com.librato.watchconf.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YAMLConverter<T> implements Converter<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public T toDomain(byte[] bytes, Class<T> clazz) throws Exception {
        return objectMapper.readValue(bytes, clazz);
    }

    @Override
    public byte[] fromDomain(T t) throws Exception {
        return objectMapper.writeValueAsBytes(t);
    }
}
