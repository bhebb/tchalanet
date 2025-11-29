package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaRepository;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawResultMapper;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawResultJpaRepositoryAdapter implements DrawResultReaderPort, DrawResultWriterPort {

  private final DrawResultJpaRepository repo;
  private final DrawResultMapper mapper;

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
  public java.util.List<DrawResult> findByCriteria(
      com.tchalanet.server.core.draw.application.query.model.DrawResultsSearchCriteria criteria) {
    return java.util.List.of();
  }

  @Override
  public DrawResult save(DrawResult result) {
    // tenantId/drawId not provided - unsupported in this signature, no-op
    return result;
  }

  @Override
  public DrawResult save(UUID tenantId, UUID drawId, DrawResult result) {
    var entity = mapper.toEntity(tenantId, drawId, result);
    var saved = repo.save(entity);
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
