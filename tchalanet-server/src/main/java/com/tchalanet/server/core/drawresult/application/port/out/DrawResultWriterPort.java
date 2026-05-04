package com.tchalanet.server.core.drawresult.application.port.out;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.Instant;

public interface DrawResultWriterPort {

  record UpsertResult(DrawResultId id, boolean created, boolean updated, boolean skippedConfirmed, boolean skippedOverridden) {}

  UpsertResult upsert(
      ResultSlotId resultSlotId,
      Instant occurredAt,
      JsonNode sourceResult,
      JsonNode haitiResult,
      JsonNode rawPayload,
      String status,
      String source,
      JsonNode flags,
      String quality,
      String sourceHash,
      String overrideReason,
      boolean force);

  /**
   * Marque un DrawResult comme OVERRIDDEN suite à une correction de résultat.
   *
   * @param drawResultId l'ID du DrawResult à marquer
   * @param reason la raison de l'override
   * @param overriddenAt timestamp de l'override
   */
  void markAsOverridden(DrawResultId drawResultId, String reason, Instant overriddenAt);
}
