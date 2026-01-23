package com.tchalanet.server.catalog.drawresult.internal.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.Instant;

public interface DrawResultWriterPort {

  record UpsertResult(DrawResultId id, boolean created, boolean updated) {}

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
}
