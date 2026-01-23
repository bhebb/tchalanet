package com.tchalanet.server.common.web.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.common.util.JsonUtils;

import java.security.MessageDigest;
import java.util.HexFormat;

public final class RequestHasher {

  private static final HexFormat HEX = HexFormat.of();

  private RequestHasher() {}

  public static String sha256Normalized(JsonUtils jsonUtils, Object body) {
    try {
      JsonNode node = jsonUtils.valueToTree(body);
      JsonNode sorted = sortRecursively(node);
      byte[] json = jsonUtils.toJson(sorted).getBytes(java.nio.charset.StandardCharsets.UTF_8);

      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(json);
      return HEX.formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot hash request body", e);
    }
  }

  private static JsonNode sortRecursively(JsonNode node) {
    if (node == null || node.isNull()) return NullNode.getInstance();

    if (node.isObject()) {
      ObjectNode out = JsonNodeFactory.instance.objectNode();
      java.util.List<String> names = new java.util.ArrayList<>();
      node.fieldNames().forEachRemaining(names::add);
      java.util.Collections.sort(names);

      for (String name : names) {
        out.set(name, sortRecursively(node.get(name)));
      }
      return out;
    }

    if (node.isArray()) {
      ArrayNode out = JsonNodeFactory.instance.arrayNode();
      for (JsonNode item : node) {
        out.add(sortRecursively(item));
      }
      return out;
    }

    return node; // value node
  }
}
