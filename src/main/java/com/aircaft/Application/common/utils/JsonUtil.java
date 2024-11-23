package com.aircaft.Application.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {
    private static final ThreadLocal<ObjectMapper> threadLocalMapper =
            ThreadLocal.withInitial(JsonUtil::createMapper);

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    public static String toJson(Object object) throws Exception {
        return threadLocalMapper.get().writeValueAsString(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return threadLocalMapper.get().readValue(json, clazz);
    }
}

