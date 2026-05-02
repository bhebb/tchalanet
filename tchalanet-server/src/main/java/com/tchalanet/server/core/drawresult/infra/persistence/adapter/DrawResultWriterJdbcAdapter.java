package com.tchalanet.server.core.drawresult.infra.persistence.adapter;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.infra.cache.DrawResultCacheEvictor;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class DrawResultWriterJdbcAdapter implements DrawResultWriterPort {

    private static final String UPSERT_SQL = """
      insert into draw_result (
        id,
        result_slot_id,
        occurred_at,
        source_result,
        haiti_result,
        raw_payload,
        status,
        source,
        flags,
        quality,
        source_hash,
        override_reason,
        fetched_at,
        created_at,
        updated_at,
        version
      )
      values (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, 0)
      on conflict (result_slot_id, occurred_at)
      do update set
        source_result = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.source_result
          else excluded.source_result
        end,
        haiti_result = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.haiti_result
          else excluded.haiti_result
        end,
        raw_payload = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.raw_payload
          else excluded.raw_payload
        end,
        status = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.status
          else excluded.status
        end,
        source = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.source
          else excluded.source
        end,
        flags = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.flags
          else excluded.flags
        end,
        quality = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.quality
          else excluded.quality
        end,
        source_hash = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.source_hash
          else excluded.source_hash
        end,
        override_reason = case
          when draw_result.status in ('CONFIRMED', 'OVERRIDDEN') and ? = false
          then draw_result.override_reason
          else excluded.override_reason
        end,
        fetched_at = excluded.fetched_at,
        updated_at = excluded.updated_at,
        version = draw_result.version + 1
      where draw_result.status not in ('CONFIRMED', 'OVERRIDDEN')
         or ? = true
      returning id, (xmax = 0) as created
      """;

    private final JdbcTemplate jdbc;
    private final JsonUtils jsonUtils;
    private final DrawResultCacheEvictor cacheEvictor;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
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
        boolean force
    ) {
        var now = Instant.now(clock);
        var newId = idGenerator.newUuid();

        var result = jdbc.query(
            UPSERT_SQL,
            rs -> {
                if (!rs.next()) {
                    return null;
                }
                var savedId = rs.getObject("id", UUID.class);
                var created = rs.getBoolean("created");
                return new UpsertResult(DrawResultId.of(savedId), created, !created);
            },
            newId,
            resultSlotId.value(),
            Timestamp.from(occurredAt),
            requiredJson(sourceResult),
            requiredJson(haitiResult),
            nullableJson(rawPayload),
            status,
            source,
            jsonOrEmpty(flags),
            quality,
            sourceHash,
            overrideReason,
            Timestamp.from(now),
            Timestamp.from(now),
            Timestamp.from(now),

            force, force, force,
            force, force, force,
            force, force, force,
            force
        );

        if (result != null) {
            AfterCommit.run(cacheEvictor::evictAll);
            return result;
        }

        var existingId = findExistingId(resultSlotId, occurredAt);
        return existingId == null ? null : new UpsertResult(existingId, false, false);
    }

    private DrawResultId findExistingId(ResultSlotId resultSlotId, Instant occurredAt) {
        var rows = jdbc.query(
            """
            select id
            from draw_result
            where result_slot_id = ?
              and occurred_at = ?
              and deleted_at is null
            """,
            (rs, rowNum) -> DrawResultId.of(rs.getObject("id", UUID.class)),
            resultSlotId.value(),
            Timestamp.from(occurredAt)
        );

        return rows.isEmpty() ? null : rows.getFirst();
    }

    private String requiredJson(JsonNode node) {
        return node == null || node.isNull() ? "{}" : jsonUtils.toJson(node);
    }

    private String nullableJson(JsonNode node) {
        return node == null || node.isNull() ? null : jsonUtils.toJson(node);
    }

    private String jsonOrEmpty(JsonNode node) {
        return node == null || node.isNull() ? "{}" : jsonUtils.toJson(node);
    }
}
