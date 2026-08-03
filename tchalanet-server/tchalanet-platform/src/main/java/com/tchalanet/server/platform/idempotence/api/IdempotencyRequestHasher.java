package com.tchalanet.server.platform.idempotence.api;

import com.tchalanet.server.common.json.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/** Produces the stable request fingerprint stored with an idempotency key. */
public final class IdempotencyRequestHasher {

  private static final HexFormat HEX = HexFormat.of();

  private IdempotencyRequestHasher() {}

  public static String sha256Normalized(JsonUtils jsonUtils, Object body) {
    try {
      JsonNode node = jsonUtils.toJsonNode(body);
      JsonNode sorted = sortRecursively(node);
      byte[] json = jsonUtils.toJson(sorted).getBytes(StandardCharsets.UTF_8);

      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HEX.formatHex(md.digest(json));
    } catch (Exception e) {
      throw new IllegalStateException("Cannot hash idempotency request", e);
    }
  }

  private static JsonNode sortRecursively(JsonNode node) {
    if (node == null || node.isNull()) return NullNode.getInstance();

    if (node.isObject()) {
      ObjectNode out = JsonUtils.emptyObject();
      var names = new java.util.ArrayList<String>();
      node.properties().forEach(entry -> names.add(entry.getKey()));
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

    return node;
  }
}
