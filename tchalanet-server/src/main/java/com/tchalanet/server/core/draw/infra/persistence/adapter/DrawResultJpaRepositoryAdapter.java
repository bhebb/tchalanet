package com.tchalanet.server.core.draw.infra.persistence.adapter;

import static com.tchalanet.server.core.draw.domain.model.DrawResultUpsertResult.*;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawResultRef;
import com.tchalanet.server.core.draw.domain.model.DrawResultUpsertResult;
import com.tchalanet.server.core.draw.infra.batch.results.common.QueryHashCalculator;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawResultMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawResultJdbcRepository;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawResultJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class DrawResultJpaRepositoryAdapter implements DrawResultReaderPort, DrawResultWriterPort {

  private final DrawResultJpaRepository repo;
  private final DrawJpaRepository drawRepo;
  private final DrawResultMapper mapper;
  private final JsonbUtils jsonbUtils;
  private final EntityManager em;
  private final DrawResultJdbcRepository drawResultJdbcRepository;

  @Override
  public Optional<DrawResult> findByDrawId(TenantId tenantId, DrawId drawId) {
    // load draw to compute local draw_date based on channel timezone
    Optional<DrawJpaEntity> drawOpt = drawRepo.findById(drawId.uuid());
    if (drawOpt.isEmpty()) return Optional.empty();
    DrawJpaEntity draw = drawOpt.get();
    var channel = draw.getDrawChannel();
    String channelCode = channel.getCode();
    // compute local date: scheduled_at at channel.timezone -> date
    ZoneId zone = ZoneId.of(channel.getTimezone() == null ? "UTC" : channel.getTimezone());
    LocalDate drawDate =
        Instant.ofEpochMilli(draw.getScheduledAt().toEpochMilli()).atZone(zone).toLocalDate();

    return repo.findByChannelCodeIgnoreCaseAndDrawDate(channelCode, drawDate).map(mapper::toDomain);
  }

  @Override
  public Optional<DrawResultRef> findByChannelCodeAndDate(String channelCode, LocalDate drawDate) {
    var ex = drawResultJdbcRepository.findExisting(channelCode, drawDate);
    if (ex == null) return Optional.empty();
    return Optional.of(new DrawResultRef(ex.id(), ex.quality()));
  }

  @Override
  public java.util.List<DrawResult> findByTenantAndDateRange(
      TenantId tenantId, java.time.LocalDate from, java.time.LocalDate to) {
    // keep behaviour: query draws for tenant and map via join on draw->channel -> draw_date ->
    // result
    if (tenantId == null || from == null || to == null) return java.util.List.of();

    String jpql =
        "select dr from DrawResultJpaEntity dr where dr.drawDate >= :fromDate and dr.drawDate <= :toDate order by dr.drawDate desc";
    TypedQuery<DrawResultJpaEntity> q = em.createQuery(jpql, DrawResultJpaEntity.class);
    q.setParameter("fromDate", from);
    q.setParameter("toDate", to);

    List<DrawResultJpaEntity> entities = q.getResultList();
    if (entities == null || entities.isEmpty()) return java.util.List.of();
    return entities.stream().map(mapper::toDomain).toList();
  }

  @Override
  public java.util.List<DrawResult> findByCriteria(DrawResultsSearchCriteria criteria) {
    if (criteria == null) return java.util.List.of();

    // For simplicity, delegate to repo queries via entity manager filtered by channelCode and date
    // range
    if (criteria.from() == null || criteria.to() == null) return java.util.List.of();

    StringBuilder jpql = new StringBuilder();
    jpql.append("select dr from DrawResultJpaEntity dr where dr.drawDate between :from and :to");
    if (criteria.channelCode() != null && !criteria.channelCode().isBlank()) {
      jpql.append(" and lower(dr.channelCode) = :code");
    }

    jpql.append(" order by dr.drawDate desc");

    TypedQuery<DrawResultJpaEntity> q = em.createQuery(jpql.toString(), DrawResultJpaEntity.class);
    // drawDate is stored as LocalDate in the entity, convert criteria ZonedDateTime -> LocalDate
    java.time.LocalDate fromDate = criteria.from().toLocalDate();
    java.time.LocalDate toDate = criteria.to().toLocalDate();
    q.setParameter("from", fromDate);
    q.setParameter("to", toDate);
    if (criteria.channelCode() != null && !criteria.channelCode().isBlank()) {
      q.setParameter("code", criteria.channelCode().trim().toLowerCase());
    }

    List<DrawResultJpaEntity> entities = q.getResultList();
    if (entities == null || entities.isEmpty()) return java.util.List.of();

    return entities.stream().map(mapper::toDomain).toList();
  }

  @Override
  public DrawResult save(TenantId tenantId, DrawResult result) {
    // Not supported: use save with draw context
    DrawResultJpaEntity entity =
        mapper.toEntity(tenantId, result); // mapper will need update if necessary
    var existing = repo.save(entity);
    return mapper.toDomain(existing);
  }

  @Override
  public DrawResult save(TenantId tenantId, DrawId drawId, DrawResult result) {
    // Upsert into canonical draw_result keyed by channelCode + drawDate
    Optional<DrawJpaEntity> drawOpt = drawRepo.findById(drawId.uuid());
    if (drawOpt.isEmpty()) return result;
    DrawJpaEntity draw = drawOpt.get();
    var channel = draw.getDrawChannel();
    String channelCode = channel.getCode();
    ZoneId zone = ZoneId.of(channel.getTimezone() == null ? "UTC" : channel.getTimezone());
    LocalDate drawDate =
        Instant.ofEpochMilli(draw.getScheduledAt().toEpochMilli()).atZone(zone).toLocalDate();

    var id = UUID.randomUUID();
    var source = result.source() == null ? "UNKNOWN" : result.source().name();
    var status = result.overridden() ? "OVERRIDDEN" : "VALID";

    // compute quality: if numbersMain present -> COMPLETE, otherwise SUSPECT
    var quality =
        (result.numbersMain() == null || result.numbersMain().isEmpty()) ? "SUSPECT" : "COMPLETE";

    String numbersMainJson =
        jsonbUtils.toJsonOrEmptyArray(
            result.numbersMain() == null ? java.util.List.of() : result.numbersMain());
    String numbersExtraJson = jsonbUtils.toJsonOrNull(result.numbersExtra());

    String rawPayloadJson = jsonbUtils.toJsonOrEmptyObject(result.rawPayload());

    java.time.Instant occurredAt =
        result.occurredAt() == null ? java.time.Instant.now() : result.occurredAt();

    repo.upsertResult(
        id,
        channelCode,
        drawDate,
        quality,
        source,
        status,
        numbersMainJson,
        numbersExtraJson,
        rawPayloadJson,
        occurredAt);

    return repo.findByChannelCodeIgnoreCaseAndDrawDate(channelCode, drawDate)
        .map(mapper::toDomain)
        .orElse(result);
  }

  @Override
  public DrawResult overrideResult(
      DrawResult result,
      com.tchalanet.server.core.draw.application.query.model.DrawResultOverrideMetadata metadata) {
    return result;
  }

  @Override
  public DrawResult invalidateResult(TenantId tenantId, DrawId drawId, String reason) {
    return null;
  }

  @Override
  public DrawResultUpsertResult upsertFromExternal(
      String channelCode,
      LocalDate drawDate,
      ExternalDrawResultPort.ExternalDrawResult ext,
      boolean force) {
    if (channelCode == null || channelCode.isBlank()) {
      throw new IllegalArgumentException("channelCode is required");
    }
    if (drawDate == null) {
      throw new IllegalArgumentException("drawDate is required");
    }
    if (ext == null) {
      throw new IllegalArgumentException("ext is required");
    }

    // normalize channel
    String ch = channelCode.trim().toUpperCase(Locale.ROOT);

    // incoming quality
    String incomingQuality = (ext.quality() == null) ? "SUSPECT" : ext.quality().name();

    // POLICY #1: refuse SUSPECT unless force
    if (!force && !"COMPLETE".equalsIgnoreCase(incomingQuality)) {
      return SKIPPED;
    }

    // Prepare payloads
    var main = (ext.numbers() == null) ? java.util.List.<String>of() : ext.numbers();
    var extra = ext.numbersExtra();
    var raw = (ext.rawPayload() == null) ? Map.<String, Object>of() : ext.rawPayload();

    String numbersMainJson = jsonbUtils.toJsonOrEmptyArray(main);
    String numbersExtraJson = jsonbUtils.toJsonOrNull(extra);
    String rawPayloadJson = jsonbUtils.toJsonOrEmptyObject(raw);

    // Determine source (EXTERNAL by default, or origin from payload)
    String source = "EXTERNAL";
    Object origin = raw.get("origin");
    if (origin != null && !origin.toString().isBlank()) {
      source = origin.toString();
    }

    Instant occurredAt = (ext.occurredAt() == null) ? Instant.now() : ext.occurredAt();

    // canonical hash
    String canonical =
        "channel="
            + ch
            + "|draw_date="
            + drawDate
            + "|numbers_main="
            + (numbersMainJson == null ? "[]" : numbersMainJson)
            + "|numbers_extra="
            + (numbersExtraJson == null ? "null" : numbersExtraJson)
            + "|occurred_at="
            + occurredAt
            + "|source="
            + source;

    String incomingHash = QueryHashCalculator.sha256Hex(canonical);

    // read existing (for downgrade/idempotence decisions)
    Optional<com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity> existingOpt =
        repo.findByChannelCodeIgnoreCaseAndDrawDate(ch, drawDate);

    if (existingOpt.isPresent()) {
      var existing = existingOpt.get();

      String existingQuality = existing.getQuality();
      String existingHash = existing.getSourceHash();

      // POLICY #2: never downgrade COMPLETE -> SUSPECT
      if ("COMPLETE".equalsIgnoreCase(existingQuality)
          && "SUSPECT".equalsIgnoreCase(incomingQuality)) {
        return SKIPPED;
      }

      // POLICY #3: idempotence COMPLETE+same hash => NOOP
      if ("COMPLETE".equalsIgnoreCase(existingQuality)
          && "COMPLETE".equalsIgnoreCase(incomingQuality)
          && existingHash != null
          && existingHash.equalsIgnoreCase(incomingHash)) {
        return NOOP;
      }
    }

    // status : MVP => VALID
    String status = "VALID";

    // do upsert (keep existing id if present, else new)
    UUID id =
        existingOpt
            .map(com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity::getId)
            .orElseGet(UUID::randomUUID);

    repo.upsertResult(
        id,
        ch,
        drawDate,
        occurredAt,
        numbersMainJson,
        numbersExtraJson,
        incomingQuality,
        status,
        source,
        incomingHash,
        rawPayloadJson);

    // determine INSERTED vs UPDATED
    if (existingOpt.isEmpty()) {
      return INSERTED;
    }
    return UPDATED;
  }
}
