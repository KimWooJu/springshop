package com.springshop.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

/**
 * Jackson ObjectMapper 래퍼.
 * 예외를 RuntimeException으로 변환하여 try-catch 부담을 줄인다.
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER;
    private static final ObjectMapper PRETTY_MAPPER;

    static {
        MAPPER = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        PRETTY_MAPPER = MAPPER.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String toJson(Object object) {
        if (object == null) return null;
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 직렬화 실패", e);
        }
    }

    public static String toPrettyJson(Object object) {
        if (object == null) return null;
        try {
            return PRETTY_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON pretty 직렬화 실패", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 역직렬화 실패: " + type.getSimpleName(), e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 역직렬화 실패", e);
        }
    }

    public static <T> List<T> fromJsonList(String json, Class<T> elementType) {
        if (json == null) return List.of();
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 리스트 역직렬화 실패", e);
        }
    }

    public static Map<String, Object> toMap(Object object) {
        if (object == null) return Map.of();
        return MAPPER.convertValue(object, new TypeReference<Map<String, Object>>() {});
    }

    public static <T> T fromMap(Map<String, Object> map, Class<T> type) {
        if (map == null) return null;
        return MAPPER.convertValue(map, type);
    }

    public static String getField(String json, String fieldName) {
        if (json == null || fieldName == null) return null;
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode field = root.get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 필드 추출 실패: " + fieldName, e);
        }
    }

    /**
     * 두 JSON 객체를 병합한다. override의 필드가 base를 덮어쓴다.
     */
    public static String merge(String base, String override) {
        try {
            JsonNode baseNode = MAPPER.readTree(base);
            JsonNode overrideNode = MAPPER.readTree(override);
            if (!(baseNode instanceof ObjectNode baseObj)) {
                return override;
            }
            overrideNode.fieldNames().forEachRemaining(name ->
                    baseObj.set(name, overrideNode.get(name)));
            return baseObj.toString();
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 병합 실패", e);
        }
    }

    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    public static class JsonException extends RuntimeException {
        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
