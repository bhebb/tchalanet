package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DrawResultJpaRepository extends JpaRepository<DrawResultJpaEntity, UUID> {

  Optional<DrawResultJpaEntity> findByChannelCodeIgnoreCaseAndDrawDate(
      String channelCode, LocalDate drawDate);

  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO draw_result (id, channel_code, draw_date, quality, source, status, numbers_main, numbers_extra, raw_payload, occurred_at, fetched_at, created_at, updated_at) VALUES (:id, :channelCode, :drawDate, :quality, :source, :status, cast(:numbersMain as jsonb), cast(:numbersExtra as jsonb), cast(:rawPayload as jsonb), :occurredAt, now(), now(), now()) ON CONFLICT (channel_code, draw_date) DO UPDATE SET quality = EXCLUDED.quality, source = EXCLUDED.source, status = EXCLUDED.status, numbers_main = EXCLUDED.numbers_main, numbers_extra = EXCLUDED.numbers_extra, raw_payload = EXCLUDED.raw_payload, occurred_at = EXCLUDED.occurred_at, updated_at = now()",
      nativeQuery = true)
  int upsertResult(
      @Param("id") UUID id,
      @Param("channelCode") String channelCode,
      @Param("drawDate") LocalDate drawDate,
      @Param("quality") String quality,
      @Param("source") String source,
      @Param("status") String status,
      @Param("numbersMain") String numbersMainJson,
      @Param("numbersExtra") String numbersExtraJson,
      @Param("rawPayload") String rawPayload,
      @Param("occurredAt") Instant occurredAt);

  @Modifying
  @Transactional
  @Query(
      value =
          """
        insert into draw_result (
          id, version,
          channel_code, draw_date,
          occurred_at,
          numbers_main, numbers_extra,
          quality, status, source,
          source_hash,
          raw_payload,
          fetched_at,
          created_at, updated_at
        )
        values (
          :id, 0,
          :channelCode, :drawDate,
          :occurredAt,
          cast(:numbersMainJson as jsonb), cast(:numbersExtraJson as jsonb),
          :quality, :status, :source,
          :sourceHash,
          cast(:rawPayloadJson as jsonb),
          now(),
          now(), now()
        )
        on conflict (channel_code, draw_date)
        do update set
          occurred_at   = excluded.occurred_at,
          numbers_main  = excluded.numbers_main,
          numbers_extra = excluded.numbers_extra,
          quality       = excluded.quality,
          status        = excluded.status,
          source        = excluded.source,
          source_hash   = excluded.source_hash,
          raw_payload   = excluded.raw_payload,
          fetched_at    = now(),
          updated_at    = now()
        """,
      nativeQuery = true)
  int upsertResult(
      @Param("id") UUID id,
      @Param("channelCode") String channelCode,
      @Param("drawDate") LocalDate drawDate,
      @Param("occurredAt") Instant occurredAt,
      @Param("numbersMainJson") String numbersMainJson,
      @Param("numbersExtraJson") String numbersExtraJson,
      @Param("quality") String quality,
      @Param("status") String status,
      @Param("source") String source,
      @Param("sourceHash") String sourceHash,
      @Param("rawPayloadJson") String rawPayloadJson);
}
