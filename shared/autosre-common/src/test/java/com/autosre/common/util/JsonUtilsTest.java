package com.autosre.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonUtilsTest {

    @Test
    void testToJson_Success() {
        TestObject obj = new TestObject("test", 123);
        String json = JsonUtils.toJson(obj);
        assertEquals("{\"name\":\"test\",\"value\":123}", json);
    }

    @Test
    void testToJson_Null() {
        String json = JsonUtils.toJson(null);
        assertEquals("null", json);
    }

    @Test
    void testToJson_Exception() {
        // A self-referencing object will cause a JsonProcessingException
        class SelfReferencing {
            public SelfReferencing self = this;
        }
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            JsonUtils.toJson(new SelfReferencing());
        });
        assertEquals("Failed to serialize object to JSON", exception.getMessage());
    }

    @Test
    void testFromJson_Success() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = JsonUtils.fromJson(json, TestObject.class);
        assertNotNull(obj);
        assertEquals("test", obj.name());
        assertEquals(123, obj.value());
    }

    @Test
    void testFromJson_InvalidJson() {
        String invalidJson = "{\"name\":\"test\","; // missing end brace
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            JsonUtils.fromJson(invalidJson, TestObject.class);
        });
        assertEquals("Failed to deserialize JSON to TestObject", exception.getMessage());
    }

    @Test
    void testGetMapper() {
        assertNotNull(JsonUtils.getMapper());
    }

    // A simple record for testing serialization/deserialization
    record TestObject(String name, int value) {}
}