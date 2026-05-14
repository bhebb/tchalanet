package com.tchalanet.server.common.json.utils;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
public class JsonUtils {

    private final JsonMapper mapper;

    /* ============================================================
     * SERIALIZATION
     * ============================================================ */

    public String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

    /* ============================================================
     * PARSE
     * ============================================================ */

    public JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to parse JSON string", e);
        }
    }

    public JsonNode parse(InputStream is) {
        try {
            return mapper.readTree(is);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to parse JSON InputStream", e);
        }
    }

    /* ============================================================
     * 🔥 SAFE METHOD (NEW STANDARD)
     * ============================================================ */

    /**
     * Convert ANY value into a proper JsonNode.
     *
     * - JsonNode → returned as-is
     * - String → parsed (NOT TextNode)
     * - Object → valueToTree
     */
    public JsonNode toJsonNode(Object value) {
        if (value == null) return null;

        if (value instanceof JsonNode node) {
            return node;
        }

        if (value instanceof String str) {
            return parse(str); // 🔥 FIX bug jsonb
        }

        try {
            return mapper.valueToTree(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to convert value to JsonNode", e);
        }
    }

    /* ============================================================
     * ⚠️ LEGACY METHODS (KEEP BUT DEPRECATE)
     * ============================================================ */

    /**
     * ⚠️ Deprecated: use toJsonNode(...) instead.
     * This method creates TextNode for String JSON.
     */
    @Deprecated
    public JsonNode valueToTree(Object value) {
        return mapper.valueToTree(value);
    }

    /**
     * ⚠️ Deprecated: use emptyObject()
     */
    @Deprecated
    public ObjectNode emptyObjectNode() {
        return mapper.createObjectNode();
    }

    /* ============================================================
     * MODERN HELPERS
     * ============================================================ */

    public ObjectNode emptyObject() {
        return mapper.createObjectNode();
    }

    public JsonNode requireObject(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalStateException("Expected JSON object but got: " + node);
        }
        return node;
    }

    /* ============================================================
     * TREE → OBJECT
     * ============================================================ */

    public <T> T treeToValue(JsonNode node, Class<T> clazz) {
        try {
            return mapper.treeToValue(node, clazz);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert JsonNode to object", e);
        }
    }

    /* ============================================================
     * GENERIC
     * ============================================================ */

    public <T> T readValue(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to read JSON from String", e);
        }
    }

    public <T> T readValue(String json, TypeReference<T> typeRef) {
        try {
            return mapper.readValue(json, typeRef);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to read JSON with TypeReference", e);
        }
    }

    public <T> T readValue(InputStream is, Class<T> clazz) {
        try {
            return mapper.readValue(is, clazz);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to read JSON from InputStream", e);
        }
    }

    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        try {
            return mapper.convertValue(fromValue, toValueTypeRef);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to convert value", e);
        }
    }

    public String toPrettyJson(Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to pretty print JSON", e);
        }
    }
}
