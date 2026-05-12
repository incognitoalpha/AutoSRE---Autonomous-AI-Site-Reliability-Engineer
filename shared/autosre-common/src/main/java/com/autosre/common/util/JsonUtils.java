package com.autosre.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class for JSON serialization and deserialization operations.
 *
 * <p>Bounded context: {@code autosre-common}</p>
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtils() {
        // Utility class, no instantiation
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param obj the object to serialize
     * @return JSON string representation
     * @throws RuntimeException if serialization fails
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Deserializes a JSON string to an object of the specified type.
     *
     * @param json the JSON string to deserialize
     * @param clazz the class of the target object
     * @param <T> the type of the object
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Returns the shared ObjectMapper instance.
     *
     * @return the ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}