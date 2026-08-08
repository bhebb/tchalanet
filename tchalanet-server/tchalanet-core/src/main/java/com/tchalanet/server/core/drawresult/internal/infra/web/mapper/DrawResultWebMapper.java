package com.tchalanet.server.core.drawresult.internal.infra.web.mapper;

import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.internal.infra.web.model.DrawResultResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class DrawResultWebMapper {

  public DrawResultResponse toResponse(DrawResultView drawResultView) {
    if (drawResultView == null) return null;
    var slotKey =
        firstNonBlank(
            drawResultView.slotKey(),
            text(drawResultView.sourceResult(), "slot_key"),
            text(drawResultView.sourceResult(), "slotKey"),
            text(drawResultView.sourceResult(), "result_slot_key"),
            text(drawResultView.haitiResult(), "slot_key"),
            text(drawResultView.rawPayload(), "slot_key"));
    var provider =
        firstNonBlank(
            providerFromSlot(slotKey),
            text(drawResultView.sourceResult(), "provider"),
            text(drawResultView.rawPayload(), "provider"));

    return new DrawResultResponse(
        drawResultView.id().toString(),
        slotKey,
        provider,
        timezoneFromSlot(slotKey, drawResultView.sourceResult(), drawResultView.rawPayload()),
        numbers(drawResultView.haitiResult(), drawResultView.sourceResult()),
        drawResultView.resultDate(),
        drawResultView.occurredAt(),
        drawResultView.status(),
        drawResultView.source(),
        drawResultView.quality(),
        drawResultView.sourceHash(),
        drawResultView.fetchedAt(),
        drawResultView.appliedAt(),
        drawResultView.sourceResult(),
        drawResultView.haitiResult(),
        drawResultView.rawPayload(),
        drawResultView.overrideReason());
  }

  public DrawResult toDomain(DrawResultResponse response) {
    if (response == null) return null;

    return new DrawResult(
        response.resultDate(),
        response.occurredAt(),
        response.status(),
        response.source(),
        response.quality(),
        response.sourceHash(),
        response.fetchedAt(),
        response.sourceResult(),
        response.haitiResult(),
        response.rawPayload(),
        response.overrideReason());
  }

  private static String providerFromSlot(String slotKey) {
    if (slotKey == null || slotKey.isBlank()) return null;
    var parts = slotKey.trim().split("[_-]", 2);
    return parts.length == 0 ? null : parts[0].toUpperCase();
  }

  private static String timezoneFromSlot(
      String slotKey, JsonNode sourceResult, JsonNode rawPayload) {
    var explicit = firstNonBlank(text(sourceResult, "timezone"), text(rawPayload, "timezone"));
    if (explicit != null) return explicit;

    return switch (providerFromSlot(slotKey)) {
      case "CA" -> "America/Los_Angeles";
      case "IL", "MO", "TN", "TX" -> "America/Chicago";
      case null, default -> "America/New_York";
    };
  }

  private static List<String> numbers(JsonNode haitiResult, JsonNode sourceResult) {
    var payload = haitiResult != null && !haitiResult.isNull() ? haitiResult : sourceResult;
    if (payload == null || payload.isNull()) return List.of();

    var out = new ArrayList<String>();
    addLotIfPresent(out, payload, "lot1", "LOT1");
    addLotIfPresent(out, payload, "lot2", "LOT2");
    addLotIfPresent(out, payload, "lot3", "LOT3");
    addLotIfPresent(out, payload, "lot4", "LOT4");
    addIfPresent(out, payload, "pick3");
    addIfPresent(out, payload, "pick4");

    return List.copyOf(out);
  }

  private static void addLotIfPresent(
      List<String> out, JsonNode payload, String flatKey, String nestedKey) {
    addIfPresent(out, payload, flatKey);
    var lots = payload.get("lots");
    if (lots != null && !lots.isNull()) {
      addIfPresent(out, lots, nestedKey);
    }
  }

  private static void addIfPresent(List<String> out, JsonNode payload, String key) {
    var value = payload.get(key);
    if (value == null || value.isNull()) return;
    var text = value.asText("").trim();
    if (!text.isBlank() && !out.contains(text)) {
      out.add(text);
    }
  }

  private static String text(JsonNode payload, String key) {
    if (payload == null || payload.isNull()) return null;
    var value = payload.get(key);
    if (value == null || value.isNull()) return null;
    var text = value.asText("").trim();
    return text.isBlank() ? null : text;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) return null;
    for (var value : values) {
      if (value != null && !value.isBlank()) return value.trim();
    }
    return null;
  }
}
