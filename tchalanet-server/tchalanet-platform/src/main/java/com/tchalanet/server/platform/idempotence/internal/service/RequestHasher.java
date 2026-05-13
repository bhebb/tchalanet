package com.tchalanet.server.platform.idempotence.internal.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
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
      node.asArray().iterator().forEachRemaining(n->names.add(n.stringValue()));
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
