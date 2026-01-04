package com.tchalanet.server.core.draw.infra.persistence.repo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DrawResultJdbcRepository {

  private final JdbcTemplate jdbc;

  public DrawResultJdbcRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public ExistingDrawResult findExisting(String channelCode, LocalDate drawDate) {
    var sql =
        """
      select id, quality, source_hash
      from draw_result
      where channel_code = ? and draw_date = ? and deleted_at is null
      """;
    var rows =
        jdbc.query(
            sql,
            (rs, i) ->
                new ExistingDrawResult(
                    (UUID) rs.getObject("id"),
                    rs.getString("quality"),
                    rs.getString("source_hash")),
            channelCode,
            drawDate);
    return rows.isEmpty() ? null : rows.get(0);
  }

  public UUID upsertReturnId(
      String channelCode,
      LocalDate drawDate,
      Instant occurredAt,
      String numbersMainJson,
      String numbersExtraJson,
      String quality,
      String status,
      String source,
      String sourceHash,
      String rawPayloadJson,
      String overrideReason,
      Instant fetchedAt,
      boolean forceUpdateComplete) {

    // IMPORTANT:
    // - we want to avoid overwriting COMPLETE unless forceUpdateComplete=true
    // - we still allow SUSPECT->COMPLETE upgrades

    var sql =
        """
      insert into draw_result(
        id, version,
        channel_code, draw_date,
        occurred_at,
        numbers_main, numbers_extra,
        quality, status, source,
        source_hash, raw_payload,
        override_reason,
        fetched_at, created_at, updated_at
      )
      values (gen_random_uuid(), 0, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?::jsonb, ?, ?, now(), now())
      on conflict (channel_code, draw_date)
      do update set
        occurred_at = excluded.occurred_at,
        numbers_main = excluded.numbers_main,
        numbers_extra = excluded.numbers_extra,
        quality = excluded.quality,
        status = excluded.status,
        source = excluded.source,
        source_hash = excluded.source_hash,
        raw_payload = excluded.raw_payload,
        override_reason = excluded.override_reason,
        fetched_at = excluded.fetched_at,
        updated_at = now()
      where draw_result.deleted_at is null
        and (
          -- allow upgrade SUSPECT -> COMPLETE
          (draw_result.quality <> 'COMPLETE' and excluded.quality = 'COMPLETE')
          or
          -- allow overwrite if forceUpdateComplete
          (? = true)
          or
          -- allow update when existing not COMPLETE (e.g. SUSPECT overwrite)
          (draw_result.quality <> 'COMPLETE')
        )
      returning id
      """;

    return jdbc.queryForObject(
        sql,
        UUID.class,
        channelCode,
        drawDate,
        occurredAt,
        numbersMainJson,
        numbersExtraJson,
        quality,
        status,
        source,
        sourceHash,
        rawPayloadJson,
        overrideReason,
        fetchedAt,
        forceUpdateComplete);
  }

  public record ExistingDrawResult(UUID id, String quality, String sourceHash) {}
}
