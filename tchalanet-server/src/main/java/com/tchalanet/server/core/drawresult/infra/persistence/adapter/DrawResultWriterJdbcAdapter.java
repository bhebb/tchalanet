package com.tchalanet.server.core.drawresult.infra.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.infra.cache.DrawResultCacheEvictor;
import com.tchalanet.server.core.drawresult.infra.persistence.repo.DrawResultJdbcRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawResultWriterJdbcAdapter implements DrawResultWriterPort {

  private final DrawResultJdbcRepository repo;
  private final JdbcTemplate jdbc;
  private final JsonUtils jsonUtils;
  private final DrawResultCacheEvictor drawResultCacheEvictor;

  @Override
  public UpsertResult upsert(
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
      boolean force) {

    // Simple upsert: try find id, if found update, else insert and return created
    var existingId = repo.findByResultSlotIdAndOccurredAt(resultSlotId.uuid(), occurredAt);
    if (existingId != null) {
      // update minimal fields
      var sql =
          "update draw_result set status = ?, quality = ?, source = ?, haiti_result = ?, raw_payload = ?, flags = ?, source_hash = ?, override_reason = ? where id = ?";
      jdbc.update(
          sql,
          status,
          quality,
          source,
          haitiResult == null ? null : jsonUtils.toJson(haitiResult),
          rawPayload == null ? null : jsonUtils.toJson(rawPayload),
          flags == null ? null : jsonUtils.toJson(flags),
          sourceHash,
          overrideReason,
          existingId);
      drawResultCacheEvictor.evictAll();
      return new UpsertResult(DrawResultId.of(existingId), false, true);
    }

    // insert
    var insertSql =
        "insert into draw_result (id, result_slot_id, occurred_at, source_result, haiti_result, raw_payload, status, source, flags, quality, source_hash, override_reason, fetched_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    UUID newId = UUID.randomUUID();
    jdbc.update(
        insertSql,
        newId,
        resultSlotId.uuid(),
        java.sql.Timestamp.from(occurredAt),
        sourceResult == null ? null : jsonUtils.toJson(sourceResult),
        haitiResult == null ? null : jsonUtils.toJson(haitiResult),
        rawPayload == null ? null : jsonUtils.toJson(rawPayload),
        status,
        source,
        flags == null ? null : jsonUtils.toJson(flags),
        quality,
        sourceHash,
        overrideReason,
        java.sql.Timestamp.from(Instant.now()));
    drawResultCacheEvictor.evictAll();
    return new UpsertResult(DrawResultId.of(newId), true, false);
  }
}
