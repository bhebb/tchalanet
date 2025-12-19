package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaRepository;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawResultMapper;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;

@Component
@RequiredArgsConstructor
public class DrawResultJpaRepositoryAdapter implements DrawResultReaderPort, DrawResultWriterPort {

  private final DrawResultJpaRepository repo;
  private final DrawResultMapper mapper;
  private final EntityManager entityManager;

  @Override
  public Optional<DrawResult> findByDrawId(UUID tenantId, UUID drawId) {
    return repo.findByTenantIdAndDrawId(tenantId, drawId).map(mapper::toDomain);
  }

  @Override
  public java.util.List<DrawResult> findByTenantAndDateRange(
      UUID tenantId, java.time.LocalDate from, java.time.LocalDate to) {
    // Not implemented: needs a query over draw dates
    return java.util.List.of();
  }

  @Override
  public java.util.List<DrawResult> findByCriteria(DrawResultsSearchCriteria criteria) {
    return java.util.List.of();
  }

  @Override
  public DrawResult save(DrawResult result) {
    // tenantId/drawId not provided - unsupported in this signature, no-op
    return result;
  }

  @Override
  public DrawResult save(UUID tenantId, UUID drawId, DrawResult result) {
    var existing = repo.findByTenantIdAndDrawId(tenantId, drawId).orElse(null);
    if (existing == null) {
      var created = mapper.toEntity(tenantId, result);
      created.setDraw(entityManager.getReference(DrawJpaEntity.class, drawId));
      var saved = repo.save(created);
      return mapper.toDomain(saved);
    }

    existing.setSource(result.source().name());
    existing.setStatus(result.overridden() ? "OVERRIDDEN" : "VALID");
    existing.setNumbersMain(result.numbersMain());
    existing.setNumbersExtra(result.numbersExtra());
    existing.setRawPayload(java.util.Map.of("raw", result.rawPayload()));
    if (result.overridden()) {
      existing.setOverriddenAt(java.time.Instant.now());
      existing.setOverrideReason(result.overrideReason());
    }

    var saved = repo.save(existing);
    return mapper.toDomain(saved);
  }

  @Override
  public DrawResult overrideResult(
      DrawResult result,
      com.tchalanet.server.core.draw.application.query.model.DrawResultOverrideMetadata metadata) {
    // basic behaviour: save as overridden
    return result;
  }

  @Override
  public DrawResult invalidateResult(UUID tenantId, UUID drawId, String reason) {
    // not implemented
    return null;
  }
}
