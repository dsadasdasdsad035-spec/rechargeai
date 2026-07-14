package com.wildai.seo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class StructuredDataService {

    private final ObjectMapper objectMapper;

    public StructuredDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toSafeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value)
                    .replace("&", "\\u0026")
                    .replace("<", "\\u003c")
                    .replace(">", "\\u003e");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("结构化数据序列化失败", ex);
        }
    }
}
