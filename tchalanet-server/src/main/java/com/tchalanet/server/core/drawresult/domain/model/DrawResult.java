package com.tchalanet.server.core.drawresult.domain.model;

import static com.tchalanet.server.common.types.enums.ResultQuality.COMPLETE;
import static com.tchalanet.server.common.types.enums.ResultQuality.SUSPECT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.common.config.ObjectMapperHolder;
import com.tchalanet.server.common.types.enums.ResultQuality;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * DrawResult (global, canonique)
 *
 * <p>Unicité: (result_slot_id, occurred_at). Contient le résultat source normalisé + la
 * projection haïtienne (lot1..lot4).
 */
public record DrawResult(
    Instant occurredAt, // instant du tirage côté source
    DrawResultStatus status, // PROVISIONAL/FINAL/ERROR
    DrawSource source, // API/MANUAL/IMPORT/ADMIN_OVERRIDE
    ResultQuality quality, // optional
    String sourceHash, // optional
    Instant fetchedAt, // required
    JsonNode sourceResult, // required (json normalisé pick3/pick4)
    JsonNode haitiResult, // required (json lots HA)
    JsonNode rawPayload, // optional
    String overrideReason // optional
    ) {

  public DrawResult {
    // Relaxed validation: allow some fields to be null for backward compatibility.
    Objects.requireNonNull(occurredAt);
    Objects.requireNonNull(status);
    Objects.requireNonNull(source);
    Objects.requireNonNull(fetchedAt);
    // sourceResult and haitiResult may be null in compatibility scenarios; callers should
    // prefer richer constructors.
  }

  /**
   * Compatibility constructor used by older code paths that previously constructed a DrawResult
   * from simple lists and source info.
   */
  public DrawResult(
      DrawSource src,
      List<String> numbersMain,
      List<String> numbersExtra,
      Instant occurredAt,
      String rawPayload,
      boolean overridden,
      String overrideReason) {
    this(
        occurredAt,
        /* status */ DrawResultStatus.VALID,
        /* source */ src,
        /* quality */ (numbersMain == null || numbersMain.isEmpty()) ? SUSPECT : COMPLETE,
        /* sourceHash */ null,
        /* fetchedAt */ occurredAt == null ? Instant.now() : occurredAt,
        /* sourceResult */ null,
        /* haitiResult */ buildHaitiResult(numbersMain, numbersExtra),
        /* rawPayload */ buildRawPayloadNode(rawPayload),
        /* overrideReason */ overrideReason);
  }

  private static JsonNode buildHaitiResult(List<String> main, List<String> extra) {
    ObjectMapper mapper = ObjectMapperHolder.get();
    if (mapper == null) return null;
    ObjectNode root = mapper.createObjectNode();
    try {
      if (main != null && !main.isEmpty()) {
        ArrayNode arr = mapper.createArrayNode();
        for (String s : main) arr.add(s);
        root.set("lot1_values", arr);
      }
      if (extra != null && !extra.isEmpty()) {
        ArrayNode arr2 = mapper.createArrayNode();
        for (String s : extra) arr2.add(s);
        root.set("extra_values", arr2);
      }
    } catch (Exception ex) {
      return null;
    }
    return root;
  }

  private static JsonNode buildRawPayloadNode(String raw) {
    if (raw == null) return null;
    ObjectMapper mapper = ObjectMapperHolder.get();
    if (mapper == null) return null;
    try {
      return mapper.readTree(raw);
    } catch (Exception e) {
      // fallback to string wrapper
      ObjectNode o = mapper.createObjectNode();
      o.put("raw", raw);
      return o;
    }
  }

  // Backwards-compatible accessors used by legacy code
  public List<String> numbersMain() {
    ObjectMapper mapper = ObjectMapperHolder.get();
    if (mapper == null) return List.of();
    try {
      if (haitiResult == null) return List.of();
      if (haitiResult.has("lot1_values") && haitiResult.get("lot1_values").isArray()) {
        ArrayNode arr = (ArrayNode) haitiResult.get("lot1_values");
        List<String> list =
            mapper.convertValue(
                arr, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        return List.copyOf(list);
      }
    } catch (Exception e) {
      // ignore
    }
    return List.of();
  }

  public List<String> numbersExtra() {
    ObjectMapper mapper = ObjectMapperHolder.get();
    if (mapper == null) return List.of();
    try {
      if (haitiResult == null) return List.of();
      if (haitiResult.has("extra_values") && haitiResult.get("extra_values").isArray()) {
        ArrayNode arr = (ArrayNode) haitiResult.get("extra_values");
        List<String> list =
            mapper.convertValue(
                arr, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        return List.copyOf(list);
      }
    } catch (Exception e) {
      // ignore
    }
    return List.of();
  }

  public boolean overridden() {
    return overrideReason != null && !overrideReason.isBlank();
  }

  public DrawResult override(List<String> newMain, List<String> newExtra, String reason) {
    JsonNode newHaiti = buildHaitiResult(newMain, newExtra);
    return new DrawResult(
        occurredAt,
        status,
        source,
        quality,
        sourceHash,
        fetchedAt,
        sourceResult,
        newHaiti,
        rawPayload,
        reason);
  }
}
