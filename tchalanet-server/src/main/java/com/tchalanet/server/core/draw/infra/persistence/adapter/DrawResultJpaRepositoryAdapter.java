package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.persistence.JsonbUtils;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawResultMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawResultJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class DrawResultJpaRepositoryAdapter implements DrawResultReaderPort, DrawResultWriterPort {

  private final DrawResultJpaRepository repo;
  private final DrawResultMapper mapper;
  private final JsonbUtils jsonbUtils;
  private final EntityManager em;

  @Override
  public Optional<DrawResult> findByDrawId(TenantId tenantId, DrawId drawId) {
    return repo.findByTenantIdAndDrawId(tenantId.uuid(), drawId.uuid()).map(mapper::toDomain);
  }

  @Override
  public java.util.List<DrawResult> findByTenantAndDateRange(
      TenantId tenantId, java.time.LocalDate from, java.time.LocalDate to) {
    if (tenantId == null || from == null || to == null) return java.util.List.of();
    // use UTC day boundaries for consistency
    var utc = java.time.ZoneId.of("UTC");
    Instant fromInstant = from.atStartOfDay(utc).toInstant();
    Instant toInstant = to.plusDays(1).atStartOfDay(utc).toInstant().minusNanos(1);

    String jpql =
        "select dr from DrawResultJpaEntity dr join dr.draw d where dr.tenantId = :tenantId and d.scheduledAt between :from and :to order by COALESCE(dr.occurredAt, d.scheduledAt) desc";
    TypedQuery<DrawResultJpaEntity> q = em.createQuery(jpql, DrawResultJpaEntity.class);
    q.setParameter("tenantId", tenantId.uuid());
    q.setParameter("from", fromInstant);
    q.setParameter("to", toInstant);

    List<DrawResultJpaEntity> entities = q.getResultList();
    if (entities == null || entities.isEmpty()) return java.util.List.of();
    return entities.stream().map(mapper::toDomain).toList();
  }

  @Override
  public java.util.List<DrawResult> findByCriteria(DrawResultsSearchCriteria criteria) {
    if (criteria == null
        || criteria.tenantId() == null
        || criteria.from() == null
        || criteria.to() == null) {
      return java.util.List.of();
    }

    var tenantUuid = criteria.tenantId().uuid();
    Instant from = criteria.from().toInstant();
    Instant to = criteria.to().toInstant();

    StringBuilder jpql = new StringBuilder();
    jpql.append("select dr from DrawResultJpaEntity dr join dr.draw d join d.drawChannel ch ");
    jpql.append("where dr.tenantId = :tenantId and d.scheduledAt between :from and :to");
    if (criteria.channelCode() != null && !criteria.channelCode().isBlank()) {
      jpql.append(" and lower(ch.code) = :code");
    }

    // order by occurredAt desc fallback to scheduledAt (latest first)
    jpql.append(" order by COALESCE(dr.occurredAt, d.scheduledAt) desc");

    TypedQuery<DrawResultJpaEntity> q = em.createQuery(jpql.toString(), DrawResultJpaEntity.class);
    q.setParameter("tenantId", tenantUuid);
    q.setParameter("from", from);
    q.setParameter("to", to);
    if (criteria.channelCode() != null && !criteria.channelCode().isBlank()) {
      q.setParameter("code", criteria.channelCode().trim().toLowerCase());
    }

    // pagination
    Integer page = criteria.page();
    Integer size = criteria.size();
    if (page != null && size != null && page >= 0 && size > 0) {
      q.setFirstResult(page * size);
      q.setMaxResults(size);
    }

    List<DrawResultJpaEntity> entities = q.getResultList();
    if (entities == null || entities.isEmpty()) return java.util.List.of();

    return entities.stream().map(mapper::toDomain).toList();
  }

  @Override
  public DrawResult save(TenantId tenantId, DrawResult result) {
    // generic save without tenant/draw context is unsupported here — delegate to caller with
    // tenant+draw
    DrawResultJpaEntity entity = mapper.toEntity(tenantId, result);
    var existing = repo.save(entity);
    return mapper.toDomain(existing);
  }

  @Override
  public DrawResult save(TenantId tenantId, DrawId drawId, DrawResult result) {
    // derive upsert parameters directly from domain DrawResult
    var id = java.util.UUID.randomUUID();
    var source = result.source() == null ? "UNKNOWN" : result.source().name();
    var status = result.overridden() ? "OVERRIDDEN" : "VALID";

    String numbersMainJson =
        jsonbUtils.toJsonOrEmptyArray(
            result.numbersMain() == null ? java.util.List.of() : result.numbersMain());
    String numbersExtraJson = jsonbUtils.toJsonOrNull(result.numbersExtra());

    String rawPayloadJson = jsonbUtils.toJsonOrEmptyObject(result.rawPayload());

    java.time.Instant occurredAt =
        result.occurredAt() == null ? java.time.Instant.now() : result.occurredAt();

    repo.upsertResult(
        id,
        tenantId.uuid(),
        drawId.uuid(),
        source,
        status,
        numbersMainJson,
        numbersExtraJson,
        rawPayloadJson,
        occurredAt);

    // return the persisted entity mapped to domain if present, otherwise return provided domain
    return repo.findByTenantIdAndDrawId(tenantId.uuid(), drawId.uuid())
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
}
