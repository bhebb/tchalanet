package com.tchalanet.server.features.publicdraw.infra.web.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.features.publicdraw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicDrawResultItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicDrawResultMapper {

  private final ObjectMapper mapper;

  public PublicDrawResultItemResponse toItem(PublicDrawResultRow r) {
    String lot1 = null, lot2 = null, lot3 = null, lot4 = null;

    try {
      if (r.getHaitiResultJson() != null && !r.getHaitiResultJson().isBlank()) {
        JsonNode j = mapper.readTree(r.getHaitiResultJson());
        lot1 = textOrNull(j, "lot1");
        lot2 = textOrNull(j, "lot2");
        lot3 = textOrNull(j, "lot3");
        lot4 = textOrNull(j, "lot4");
      }
    } catch (Exception ignore) {
    }

    return new PublicDrawResultItemResponse(
        r.getSlotKey(),
        r.getProvider(),
        r.getSlotTimezone(),
        r.getSlotDrawTime() == null ? null : r.getSlotDrawTime().toString(),
        r.getDrawDate(),
        r.getOccurredAt(),
        lot1,
        lot2,
        lot3,
        lot4,
        r.getStatus() == null ? null : r.getStatus(),
        r.getQuality() == null ? null : r.getQuality(),
        r.getSource());
  }

  private static String textOrNull(JsonNode j, String key) {
    JsonNode n = j.get(key);
    if (n == null || n.isNull()) return null;
    String s = n.asText();
    return (s == null || s.isBlank()) ? null : s;
  }
}
