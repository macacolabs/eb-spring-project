package com.ohgiraffers.restapi.util;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

public class ConvertUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertObjectToMap(Object obj){
        return OBJECT_MAPPER.convertValue(obj, Map.class);
    }
}
